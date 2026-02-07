package com.example.vericert.controller;

import com.example.vericert.component.PaypalClient;
import com.example.vericert.domain.Payment;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.domain.User;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UserRepository;
import com.example.vericert.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private final MailService mailService;
    private final UserRepository userRepo;
    private final PaymentRepository paymentRepo;


    public BillingController(BillingService billingService,
                             PlanEnforcementService planEnforcementService,
                             PlanDefinitionRepository planRepo,
                             PaypalSubscriptionService paypalSubscriptionService,
                             TenantSettingsRepository tenantSettingsRepo,
                             PaypalClient paypalClient,
                             MailService mailService,
                             UserRepository userRepo,
                             PaymentRepository paymentRepo) {
        this.billingService = billingService;
        this.planEnforcementService = planEnforcementService;
        this.planRepo = planRepo;
        this.paypalSubscriptionService = paypalSubscriptionService;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.paypalClient = paypalClient;
        this.mailService = mailService;
        this.userRepo = userRepo;
        this.paymentRepo = paymentRepo;
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
        model.addAttribute("title", "Pagamento riuscito");
        model.addAttribute("description", "Pagamento riuscito.");
        return "billing/success"; // templates/billing/success.html
    }

    @GetMapping("/cancel")
    public String cancel(Model model) {

        model.addAttribute("title", "Pagamento annullato");
        model.addAttribute("description", "Pagamento annullato.");
        return "billing/cancel";  // templates/billing/cancel.html
    }


    @GetMapping("/paypal/success")
    public String successPaypal(@RequestParam("subscription_id") String subscriptionId,
                                @RequestParam(value = "token", required = false) String token,
                                Model model) throws JsonProcessingException {
        // - currentPlan (CurrentPlanView)
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(currentTenantId());
        Map<String, Object> s = paypalClient.get("/v1/billing/subscriptions/" + subscriptionId, Map.class);
        String status = String.valueOf(s.get("status"));
        TenantSettings ts = tenantSettingsRepo.findByTenantId(currentTenantId()).orElseThrow();
        //CALCOLO DATE
        //CALCOLO DATE
        Instant now = Instant.now();

        Instant start = ts.getCurrentPeriodStart();
        Instant end   = ts.getCurrentPeriodEnd();

        Instant newStart;
        if (now.isBefore(start)) {
            // periodo assurdo nel futuro: riallinea
            newStart = now;
        } else if (now.isBefore(end)) {
            // siamo ancora nel periodo corrente: NON rinnovare ancora (skip)
            model.addAttribute("title", "Pagamento annullato");
            model.addAttribute("description", "Pagamento annullato.");
            return "billing/cancel";
        } else {
            // now >= end: rinnovo normale
            newStart = end;
        }
        Instant newEnd = computeNextPeriodEnd(newStart, ts.getBillingCycle());
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            // non ancora attiva: pagina di attesa
            model.addAttribute("provider", "PAYPAL");
            model.addAttribute("subscriptionId", subscriptionId);
            model.addAttribute("autoRefresh", true);
            model.addAttribute("paypalStatus", status);
            model.addAttribute("title", "Pagamento in stato sospeso");
            model.addAttribute("description", "Pagamento in stato sospeso.");
            return "billing/pending";
        }
        Map<String, Object> billingInfo = (Map<String, Object>) s.get("billing_info");
        Map<String, Object> lastPayment = (Map<String, Object>) billingInfo.get("last_payment");
        String lastPaymentTime = String.valueOf(lastPayment.get("time")); // ISO

        String idem = "PAYPAL:RENEW:" + subscriptionId + ":" + lastPaymentTime;

        Payment pay = new Payment();
        pay.setTenantId(currentTenantId());
        pay.setProvider("PAYPAL");
        pay.setProviderIntentId(subscriptionId);
        pay.setStatus("SUCCEEDED");
        pay.setCurrency("EUR");
        pay.setAmountMinor(0L);
        pay.setIdempotencyKey(idem);
        if (!paymentRepo.existsByIdempotencyKey(idem)) {
            paymentRepo.saveAndFlush(pay);
        }
        else{
            pay = paymentRepo.findByProviderIntentId(subscriptionId).orElseThrow();
        }
        billingService.renewPaypalSubscription(currentTenantId(), currentPlan.getPlanCode(), currentPlan.getBillingCycle(), subscriptionId, idem, newStart, newEnd);
        //SEND MAIL
        if(!pay.isRenewEmailSentPaypal()){
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User u = userRepo.findByUserName(username).orElseThrow();
            // invio email acquisto (idempotenza consigliata)
            String to = u.getEmail();
            Instant date = tenantSettingsRepo.findByTenantId(currentTenantId()).get().getCurrentPeriodEnd();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
            String formatted = fmt.format(date);
            Map<String, Object> amount = lastPayment != null ? (Map<String, Object>) lastPayment.get("amount") : null;
            String paidValue = amount != null ? formatEuroIT(String.valueOf((amount.get("value")))) : null;        // "0.61"
            String paidCurrency = amount != null ? String.valueOf(amount.get("currency_code")) : null; // "EUR"
            Map<String,Object> vars = new HashMap<>();
            vars.put("action", "Rinnovo");
            vars.put("customer_name", u.getFullName() != null ? u.getFullName() : username);
            vars.put("paid_at", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.of("Europe/Rome")).format(Instant.now()));
            vars.put("payment_ref", ts.getSubscriptionId());
            vars.put("provider", "PayPal");
            // importi (se p.amountMinor è lordo)
            String valueStr = amount != null ? String.valueOf(amount.get("value")) : null; // "0.61"
            BigDecimal total = valueStr != null ? new BigDecimal(valueStr).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal net = total.divide(new BigDecimal("1.22"), 2, RoundingMode.HALF_UP);
            BigDecimal vat = total.subtract(net).setScale(2, RoundingMode.HALF_UP);
            vars.put("amount_net", formatEuroIT(String.valueOf(net)));
            vars.put("vat_amount", formatEuroIT(String.valueOf(vat)));
            vars.put("amount_total", formatEuroIT(String.valueOf(total)));
            vars.put("subscription_id", subscriptionId);
            // plan/cycle: prendili da tenant_settings o currentPlanView
            vars.put("plan_name", currentPlan.getPlanCode());
            vars.put("billing_cycle", currentPlan.getBillingCycle());
            vars.put("portal_url", "https://app.vercert.org/");
            vars.put("support_email", "support@app.vercert.org");
            vars.put("company_name", "VeriCert");
            vars.put("company_address", "…");
            mailService.sendPurchaseSuccess(to,"Rinnovo pagamento per piano - " + currentPlan.getPlanCode() , vars);
            pay.setRenewEmailSentPaypal(true);
            paymentRepo.save(pay);
            model.addAttribute("provider", "PAYPAL");
            model.addAttribute("status", "ACTIVE");
            model.addAttribute("currentPlan", currentPlan);
            model.addAttribute("amountFormatted", paidValue);
            model.addAttribute("nextRenewalDate", formatted);
            model.addAttribute("transactionId", subscriptionId); // o l’id PayPal/Stripe real
            model.addAttribute("title", "Pagamento riuscito");
            model.addAttribute("description", "Pagamento riuscito.");
        }
        return "billing/success";
    }
    @GetMapping("/paypal/cancel")
    public String cancelPaypal(Model model) {

        model.addAttribute("title", "Pagamento annullato");
        model.addAttribute("description", "Pagamento annullato.");
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
    private Instant computeNextPeriodEnd(Instant start, String cycle) {
        ZonedDateTime z = start.atZone(ZoneId.of("Europe/Rome"));
        return switch (String.valueOf(cycle).toUpperCase()) {
            case "YEARLY", "ANNUAL", "ANNUALLY" -> z.plusYears(1).toInstant();
            default -> z.plusMonths(1).toInstant(); // MONTHLY
        };
    }
}