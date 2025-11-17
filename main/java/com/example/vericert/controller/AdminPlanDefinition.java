package com.example.vericert.controller;

import com.example.vericert.domain.PlanDefinitions;
import com.example.vericert.service.AdminPlanDefinitionsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminPlanDefinition {

    private final AdminPlanDefinitionsService service;

    public AdminPlanDefinition(AdminPlanDefinitionsService service) {
        this.service = service;
    }
    @GetMapping("/")
    public String home(@RequestParam(required = false,defaultValue = "false") boolean annual,
                       Model model)
    {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("billingAnnual", annual);
        PlanDefinitions p = service.getPlan("FREE".toUpperCase());
        if(p != null)
        {
            List<PlanDefinitions> items = service.getPlans();
            model.addAttribute("items", items);
        }

        return "index";
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
