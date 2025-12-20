package com.example.vericert.controller;

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
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.access.prepost.PreAuthorize;
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
import static com.example.vericert.util.PdfUtil.formatCents;

@Controller
@RequestMapping("/billing")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BillingController {

    private final BillingService billingService;
    private final PlanEnforcementService planEnforcementService;
    private final PlanDefinitionRepository planRepo;
    private final PaypalSubscriptionService paypalSubscriptionService;
    private final TenantSettingsRepository tenantSettingsRepo;

    public BillingController(BillingService billingService,
                             PlanEnforcementService planEnforcementService,
                             PlanDefinitionRepository planRepo,
                             PaypalSubscriptionService paypalSubscriptionService,
                             TenantSettingsRepository tenantSettingsRepo) {
        this.billingService = billingService;
        this.planEnforcementService = planEnforcementService;
        this.planRepo = planRepo;
        this.paypalSubscriptionService = paypalSubscriptionService;
        this.tenantSettingsRepo = tenantSettingsRepo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String cancel() {

        return "billing/cancel";  // templates/billing/cancel.html
    }


    @GetMapping("/paypal/success")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String successPaypal(@RequestParam("subscription_id") String subscriptionId,
                                @RequestParam(value = "token", required = false) String token,
                       Model model) {

        // Recupero info subscription da PayPal
        PaypalSubscriptionDto sub = paypalSubscriptionService.findById(subscriptionId);
        // se vuoi, recuperi info da Stripe/PayPal qui e calcoli:
        // - provider
        // - amountFormatted
        // - nextRenewalDate
        // - transactionId
        // - currentPlan (CurrentPlanView)
        Instant date = tenantSettingsRepo.findByTenantId(currentTenantId()).get().getCurrentPeriodEnd();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(date);
        CurrentPlanView currentPlan = planEnforcementService.buildCurrentPlanView(currentTenantId());
        model.addAttribute("currentPlan", currentPlan);
        model.addAttribute("amountFormatted",formatEuroIT(sub.amountValue()));
        model.addAttribute("provider", "PayPal"); // o "PayPal"
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", subscriptionId); // o l’id PayPal/Stripe reale
        return "billing/success";
    }
    @GetMapping("/paypal/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
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
