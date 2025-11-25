package com.example.vericert.controller;

import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.dto.CurrentPlanView;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.vericert.util.PdfUtil.formatCents;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final PlanEnforcementService planEnforcementService;
    private final PlanDefinitionRepository planRepo;

    public BillingController(BillingService billingService,
                             PlanEnforcementService planEnforcementService,
                             PlanDefinitionRepository planRepo) {
        this.billingService = billingService;
        this.planEnforcementService = planEnforcementService;
        this.planRepo = planRepo;
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
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("amount", formatCents(session.getAmountTotal()));
        model.addAttribute("currency", session.getCurrency());
        return "billing/success"; // templates/billing/success.html
    }
    @GetMapping("/cancel")
    public String cancel() {
        return "billing/cancel";  // templates/billing/cancel.html
    }
}
