package com.example.vericert.controller;

import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.dto.CurrentPlanView;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
