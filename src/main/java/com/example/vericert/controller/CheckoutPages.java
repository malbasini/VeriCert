package com.example.vericert.controller;

import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.PlanEnforcementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.example.vericert.util.PdfUtil.formatCents;

@Controller
public class CheckoutPages {

    private final PaymentRepository payRepo;
    private final PlanEnforcementService planEnforcementService;
    private final TenantSettingsRepository tenantSettingsRepo;

    public CheckoutPages(PaymentRepository payRepo,
                       PlanEnforcementService planEnforcementService,
                         TenantSettingsRepository tenantSettingsRepo) {
        this.payRepo = payRepo;
        this.planEnforcementService = planEnforcementService;
        this.tenantSettingsRepo = tenantSettingsRepo;

    }
    @GetMapping("/checkout/success")
    public String ok(@RequestParam("session_id") String sessionId, Model model) throws Exception {
        var session = com.stripe.model.checkout.Session.retrieve(sessionId);
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
        model.addAttribute("amountFormatted",formatCents(session.getAmountTotal()));
        model.addAttribute("provider", "Stripe"); // o "PayPal"
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", sessionId); // o l’id PayPal/Stripe reale
        model.addAttribute("title", "Pagamento riuscito");
        model.addAttribute("description", "Pagamento riuscito.");
        return "stripe/success";
    }
    @GetMapping("/checkout/cancel")
    public String cancel() {
        return "stripe/cancel";
    }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}

