package com.example.vericert.controller;

import com.example.vericert.component.PaypalClient;
import com.example.vericert.dto.PaypalSubscriptionDto;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PaypalSubscriptionService;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.dto.CurrentPlanView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;


import static com.example.vericert.util.PdfUtil.formatCents;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final PlanEnforcementService planEnforcementService;
    private final PlanDefinitionRepository planRepo;
    private final PaypalSubscriptionService paypalSubscriptionService;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final PaypalClient paypalClient;


    public BillingController(BillingService billingService,
                             PlanEnforcementService planEnforcementService,
                             PlanDefinitionRepository planRepo,
                             PaypalSubscriptionService paypalSubscriptionService,
                             TenantSettingsRepository tenantSettingsRepo,
                             PaypalClient paypalClient) {
        this.billingService = billingService;
        this.planEnforcementService = planEnforcementService;
        this.planRepo = planRepo;
        this.paypalSubscriptionService = paypalSubscriptionService;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.paypalClient = paypalClient;
    }

    @GetMapping
    public String billingHome(Model model) {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long tenantId = user.getTenantId();
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(tenantId);
        List<PlanDefinition> plans = planRepo.findAll();
        model.addAttribute("currentPlan", currentPlan);
        model.addAttribute("plans", plans);
        return "billing/plans";  // es: templates/billing/plans.html
    }

    @PostMapping("/checkout")
    public String startCheckout(@RequestParam String planCode,
                                @RequestParam String billingCycle,   // MONTHLY/ANNUAL
                                @RequestParam BillingProvider provider) {

        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long tenantId = user.getTenantId();
        String redirectUrl = billingService.startCheckout(tenantId, planCode, billingCycle, provider);
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/success")
    public String success(@RequestParam("session_id") String sessionId, Model model) throws StripeException {
        // opzionale: recuperare la sessione da Stripe e mostrare info
        Session session = Session.retrieve(sessionId);
        Instant date = tenantSettingsRepo.findByTenantId(currentTenantId()).get().getCurrentPeriodEnd();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(date);
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(currentTenantId());
        model.addAttribute("currentPlan", currentPlan);
        model.addAttribute("amountFormatted",formatCents(session.getAmountTotal()));
        model.addAttribute("provider", "Stripe"); // o "PayPal"
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", sessionId); // o l’id PayPal/Stripe reale
        return "billing/success"; // templates/billing/success.html
    }

    @GetMapping("/cancel")
    public String cancel() {

        return "billing/cancel";  // templates/billing/cancel.html
    }


    @GetMapping("/paypal/success")
    public String successPaypal(@RequestParam("subscription_id") String subscriptionId,
                                @RequestParam(value = "token", required = false) String token,
                       Model model) throws JsonProcessingException {
        // Recupero info subscription da PayPal
        PaypalSubscriptionDto sub = paypalSubscriptionService.findById(subscriptionId);
        // se vuoi, recuperi info da Stripe/PayPal qui e calcoli:
        // - provider
        // - amountFormatted
        // - nextRenewalDate
        // - transactionId
        // - currentPlan (CurrentPlanView)
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(currentTenantId());
        ObjectMapper om = new ObjectMapper();
        Map<String, Object> s = paypalClient.get("/v1/billing/subscriptions/" + subscriptionId, Map.class);
        String status = String.valueOf(s.get("status"));

        if (!"ACTIVE".equalsIgnoreCase(status)) {
            // non ancora attiva: pagina di attesa
            model.addAttribute("provider", "PAYPAL");
            model.addAttribute("subscriptionId", subscriptionId);
            model.addAttribute("autoRefresh", true);
            model.addAttribute("paypalStatus", status);
            return "billing/pending";
        }

        // --- custom_id può essere String o Map (dipende da come l’hai inviato) ---
        Object customObj = s.get("custom_id");
        Map<String, String> meta;

        if (customObj instanceof String str) {
            meta = om.readValue(str, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } else if (customObj instanceof Map<?,?> m) {
            meta = new java.util.HashMap<>();
            for (var e : m.entrySet()) {
                meta.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        } else {
            meta = java.util.Map.of();
        }

        String tenantIdStr = meta.get("tenant_id");
        String planCode = meta.get("plan_code");
        String billingCycle = meta.get("billing_cycle");

        if (tenantIdStr == null || planCode == null || billingCycle == null) {
            // QUI è dove ti esce "metadati mancanti"
            model.addAttribute("message", "Metadati PayPal mancanti (custom_id).");
            return "billing/error";
        }

        Long tenantId = Long.valueOf(tenantIdStr);

         // Idempotenza: evita doppie attivazioni se refreshi la pagina
         // (es. se già ACTIVE e providerSubscriptionId uguale -> return ok)
        billingService.activateSubscription(
                tenantId,
                planCode,
                billingCycle,
                BillingProvider.PAYPAL,
                subscriptionId,
                null
        );
        Instant date = tenantSettingsRepo.findByTenantId(currentTenantId()).get().getCurrentPeriodEnd();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(date);
        @SuppressWarnings("unchecked")
        Map<String, Object> billingInfo = (Map<String, Object>) s.get("billing_info");
        @SuppressWarnings("unchecked")
        Map<String, Object> lastPayment = billingInfo != null ? (Map<String, Object>) billingInfo.get("last_payment") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> amount = lastPayment != null ? (Map<String, Object>) lastPayment.get("amount") : null;
        String paidValue = amount != null ? formatEuroIT(String.valueOf((amount.get("value")))) : null;        // "0.61"
        String paidCurrency = amount != null ? String.valueOf(amount.get("currency_code")) : null; // "EUR"
        model.addAttribute("provider", "PAYPAL");
        model.addAttribute("status", "ACTIVE");
        model.addAttribute("currentPlan", currentPlan);
        model.addAttribute("amountFormatted", paidValue);
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", subscriptionId); // o l’id PayPal/Stripe reale
        return "billing/success";
    }
    @GetMapping("/paypal/cancel")
    public String cancelPaypal() {
        return "billing/cancel";
    }

     public static String formatEuroIT(String importoStr) {
         BigDecimal value = new BigDecimal(importoStr.replace(',', '.')); // gestisce input "12.08" o "12,08"
         NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
         return fmt.format(value); // "€ 12,08"
     }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
