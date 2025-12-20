package com.example.vericert.controller;

import com.example.vericert.dto.UsageAndLimitsView;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.TenantSettingsService;
import com.example.vericert.dto.TenantSettingsDto;
import com.example.vericert.service.UsageLimitsViewService;
import com.example.vericert.service.UsageMeterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminSettingsController {

    private final TenantSettingsService service;
    private final UsageLimitsViewService usageLimitsViewService;

    public AdminSettingsController(TenantSettingsService service,
                                   UsageLimitsViewService usageLimitsViewService)
    {

        this.service = service;
        this.usageLimitsViewService = usageLimitsViewService;
    }

    private Long currentTenantId(Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    @GetMapping
    public String show(Model model, Authentication auth) {
        Long tenantId = currentTenantId(auth);
        TenantSettingsDto dto = service.loadForTenant(tenantId);
        model.addAttribute("form", dto);
        model.addAttribute("tenantId", tenantId);
        var view = usageLimitsViewService.buildUsageAndLimits(tenantId);
        model.addAttribute("usage", view);
        return "settings/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") TenantSettingsDto form,
                      BindingResult br,
                       Model model,
                       Authentication auth
    )
    {
        if (br.hasErrors()) {
            Long tenantId = currentTenantId(auth);
            var view = usageLimitsViewService.buildUsageAndLimits(tenantId);
            model.addAttribute("usage", view);
            return "settings/form";

        }
        Long tenantId = currentTenantId(auth);
        service.saveForTenant(tenantId, form);
        model.addAttribute("saved", true);
        var view = usageLimitsViewService.buildUsageAndLimits(tenantId);
        model.addAttribute("usage", view);
        return "settings/form";
    }
}