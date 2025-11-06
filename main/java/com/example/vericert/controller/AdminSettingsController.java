package com.example.vericert.controller;

import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.TenantSettingsService;
import com.example.vericert.dto.TenantSettingsDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN','ISSUER')")
public class AdminSettingsController {

    private final TenantSettingsService service;

    public AdminSettingsController(TenantSettingsService service) {
        this.service = service;
    }

    private Long currentTenantId(Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId(); // Assicurati di averlo sul CustomUserDetails
    }

    @GetMapping
    public String show(Model model, Authentication auth) {
        Long tenantId = currentTenantId(auth);
        TenantSettingsDto dto = service.loadForTenant(tenantId);

        model.addAttribute("form", dto);

        // TODO: puoi anche caricare "usage" dal tuo UsageMeterService e metterlo nel model:
        // model.addAttribute("usage", usageMeterService.getMonthlyUsage(tenantId));
        // Per ora fingi:
        model.addAttribute("usage", new UsageStub(12,20,"5.2 MB","100 MB"));

        return "settings/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") TenantSettingsDto form,
            BindingResult br,
            Model model,
            Authentication auth
    ) {
        if (br.hasErrors()) {
            model.addAttribute("usage", new UsageStub(12,20,"5.2 MB","100 MB"));
            return "settings/form";
        }

        Long tenantId = currentTenantId(auth);
        service.saveForTenant(tenantId, form);

        model.addAttribute("saved", true);
        model.addAttribute("usage", new UsageStub(12,20,"5.2 MB","100 MB"));
        return "settings/form";
    }

    // Stub temporaneo per mostrare quote in pagina
    public record UsageStub(
            int issuedThisMonth,
            int monthlyLimit,
            String storageUsed,
            String storageLimit
    ) {}
}