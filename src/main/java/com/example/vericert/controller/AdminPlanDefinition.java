package com.example.vericert.controller;

import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
public class AdminPlanDefinition {

    private final AdminPlanDefinitionsService service;
    private final TenantSettingsRepository tenantSettingsRepository;
    public AdminPlanDefinition(AdminPlanDefinitionsService service, TenantSettingsRepository tenantSettingsRepository)
    {
        this.service = service;
        this.tenantSettingsRepository = tenantSettingsRepository;
    }
    @GetMapping("/pricing")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
    public String home(@SessionAttribute(name = "billingAnnual", required = false) Boolean annual,
                       Model model)
    {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        boolean billingAnnual = Boolean.TRUE.equals(annual); // default false se null
        model.addAttribute("billingAnnual", billingAnnual);
        PlanDefinition p = service.getPlan("FREE".toUpperCase());
        //Imposto la mail in tenant_settings con l'utente attualmente loggato.
        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId).orElse(null);
        if(ts != null)
        {
            ts.setEmail(currentUserEmail());
            tenantSettingsRepository.save(ts);
        }
        if(p != null)
        {
            List<PlanDefinition> items = service.getPlans();
            model.addAttribute("items", items);
        }
        if(billingAnnual)
            model.addAttribute("period", "ANNUAL");
        else
            model.addAttribute("period", "MONTHLY");

        model.addAttribute("title", "Prezzi Vercert | Piani per certificati digitali verificabili con QR");
        model.addAttribute("description", "Confronta i piani Vercert per aziende: emissione e verifica certificati digitali con QR code. Mensile o annuale con sconto.");
        return "pricing";
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
    private String currentUserEmail() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getEmail();
    }

    @PostMapping("/pricing/billing")
    @ResponseBody
    public Map<String, Object> setBilling(@RequestParam boolean annual, HttpSession session) {
        session.setAttribute("billingAnnual", annual);
        return Map.of("ok", true, "annual", annual);
    }

}
