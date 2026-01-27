package com.example.vericert.controller;

import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.Tenant;
import com.example.vericert.repo.SigningKeyRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSigningKeyRepository;
import com.example.vericert.service.TenantSigningKeyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/signing-keys")
public class AdminSigningKeyViewController {

    private final TenantSigningKeyRepository tenantSigningKeyRepo;
    private final SigningKeyRepository signingKeyRepo;
    private final TenantRepository tenantRepo;
    private final TenantSigningKeyService tenantSigningKeyService;

    public AdminSigningKeyViewController(
            TenantSigningKeyRepository tenantSigningKeyRepo,
            SigningKeyRepository signingKeyRepo,
            TenantRepository tenantRepo,
            TenantSigningKeyService tenantSigningKeyService
    ) {
        this.tenantSigningKeyRepo = tenantSigningKeyRepo;
        this.signingKeyRepo = signingKeyRepo;
        this.tenantRepo = tenantRepo;
        this.tenantSigningKeyService = tenantSigningKeyService;
    }

    @GetMapping("/{tenantId}")
    public String view(@PathVariable Long tenantId, Model model) {

        Tenant tenant = tenantRepo.getById(tenantId);

        var tskOpt = tenantSigningKeyRepo.findByTenantId(tenantId);
        SigningKeyEntity signingKey = null;

        if (tskOpt != null) {
            signingKey = signingKeyRepo.findById(tskOpt.getKid()).orElse(null);
        }

        model.addAttribute("tenant", tenant);
        model.addAttribute("signingKey", signingKey);

        return "admin/signing-key";
    }

    @PostMapping("/{tenantId}/generate")
    public String generate(@PathVariable Long tenantId) throws Exception {
        Tenant tenant = tenantRepo.getById(tenantId);
        tenantSigningKeyService.ensureTenantKey(tenantId, tenant.getName());
        return "redirect:/admin/signing-key/" + tenantId;
    }

    @PostMapping("/{tenantId}/rotate")
    public String rotate(@PathVariable Long tenantId) throws Exception {
        Tenant tenant = tenantRepo.getById(tenantId);
        tenantSigningKeyService.rotateTenantKey(tenantId, tenant.getName());
        return "redirect:/admin/signing-key/" + tenantId;
    }
}
