package com.example.vericert.controller;

import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.service.AdminPlanDefinitionsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
public class AdminPlanDefinition {

    private final AdminPlanDefinitionsService service;

    public AdminPlanDefinition(AdminPlanDefinitionsService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home(@SessionAttribute(name = "billingAnnual", required = false) Boolean annual,
                       Model model)
    {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        boolean billingAnnual = Boolean.TRUE.equals(annual); // default false se null
        model.addAttribute("billingAnnual", billingAnnual);
        PlanDefinition p = service.getPlan("FREE".toUpperCase());
        if(p != null)
        {
            List<PlanDefinition> items = service.getPlans();
            model.addAttribute("items", items);
        }
        if(billingAnnual)
            model.addAttribute("period", "ANNUAL");
        else
            model.addAttribute("period", "MONTHLY");
        return "index";
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    @PostMapping("/pricing/billing")
    @ResponseBody
    public Map<String, Object> setBilling(@RequestParam boolean annual, HttpSession session) {
        session.setAttribute("billingAnnual", annual);
        return Map.of("ok", true, "annual", annual);
    }

}
