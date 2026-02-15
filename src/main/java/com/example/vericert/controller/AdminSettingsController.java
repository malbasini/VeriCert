package com.example.vericert.controller;

import com.example.vericert.domain.TenantProfile;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.dto.TenantProfileForm;
import com.example.vericert.dto.TenantSettingsDto;
import com.example.vericert.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminSettingsController {

    private final TenantSettingsService service;
    private final UsageLimitsViewService usageLimitsViewService;
    private final TenantProfileService tenantProfileService;
    private final TenantAssetStorageService storageService;

    public AdminSettingsController(TenantSettingsService service,
                                   UsageLimitsViewService usageLimitsViewService,
                                   TenantProfileService tenantProfileService,
                                   TenantAssetStorageService storageService)
    {

        this.service = service;
        this.usageLimitsViewService = usageLimitsViewService;
        this.tenantProfileService = tenantProfileService;
        this.storageService = storageService;
    }

    private Long currentTenantId(Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    @GetMapping
    public String show(Model model, Authentication auth) {
        Long tenantId = currentTenantId(auth);
        TenantSettingsDto dto = service.loadForTenant(tenantId);
        CurrentPlanView view = usageLimitsViewService.buildUsageAndLimits(tenantId);
        TenantProfile p = tenantProfileService.getOrCreate(tenantId);
        model.addAttribute("form", dto);
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("billingProfile", TenantProfileService.toForm(p));
        model.addAttribute("activeTab", "general");
        model.addAttribute("usage", view);
        return "settings/form";
    }

    @PostMapping("/save")
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

        // Preserva logo e firma esistenti se non vengono inviati dal form
        TenantSettingsDto currentSettings = service.loadForTenant(tenantId);
        TenantSettingsDto mergedSettings = new TenantSettingsDto(
                form.displayName(),
                form.contactEmail(),
                form.website(),
                form.issuerName(),
                form.issuerRole(),
                // Preserva logo e firma esistenti
                (form.logoUrl() == null || form.logoUrl().isEmpty())
                    ? currentSettings.logoUrl()
                    : form.logoUrl(),
                (form.signatureImageUrl() == null || form.signatureImageUrl().isEmpty())
                    ? currentSettings.signatureImageUrl()
                    : form.signatureImageUrl(),
                form.primaryColor(),
                form.defaultTemplateId()
        );

        service.saveForTenant(tenantId, mergedSettings);
        TenantSettingsDto dto = service.loadForTenant(tenantId);
        TenantProfile p = tenantProfileService.getOrCreate(tenantId);
        model.addAttribute("saved", true);
        var view = usageLimitsViewService.buildUsageAndLimits(tenantId);
        model.addAttribute("billingProfile", TenantProfileService.toForm(p));
        model.addAttribute("usage", view);
        model.addAttribute("form", dto);
        model.addAttribute("tenantId", tenantId);
        return "settings/form";
    }
    @PostMapping("/profile")
    public String saveTenantProfile(@ModelAttribute("billingProfile") TenantProfileForm billingProfile,
                       Model model,
                       Authentication auth
    )
    {
        Long tenantId = currentTenantId(auth);
        TenantSettingsDto dto = service.loadForTenant(tenantId);
        TenantProfile p = tenantProfileService.update(tenantId,billingProfile);
        model.addAttribute("saved", true);
        var view = usageLimitsViewService.buildUsageAndLimits(tenantId);
        model.addAttribute("billingProfile", TenantProfileService.toForm(p));
        model.addAttribute("usage", view);
        model.addAttribute("form", dto);
        model.addAttribute("tenantId", tenantId);
        return "settings/form";
    }

    @PostMapping("/upload-logo")
    @ResponseBody
    public ResponseEntity<?> uploadLogo(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            Long tenantId = currentTenantId(auth);
            storageService.saveLogo(tenantId, file);
            String logoUrl = storageService.logoPublicUrl(tenantId);
            // Aggiorna le impostazioni con il nuovo URL
            TenantSettingsDto currentSettings = service.loadForTenant(tenantId);
            TenantSettingsDto updatedSettings = new TenantSettingsDto(
                    currentSettings.displayName(),
                    currentSettings.contactEmail(),
                    currentSettings.website(),
                    currentSettings.issuerName(),
                    currentSettings.issuerRole(),
                    logoUrl,
                    currentSettings.signatureImageUrl(),
                    currentSettings.primaryColor(),
                    currentSettings.defaultTemplateId()
            );
            service.saveForTenant(tenantId, updatedSettings);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("url", logoUrl);
            response.put("message", "Logo caricato con successo");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Errore durante l'upload: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/upload-signature")
    @ResponseBody
    public ResponseEntity<?> uploadSignature(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            Long tenantId = currentTenantId(auth);
            storageService.saveSignature(tenantId,file);
            String signatureUrl = storageService.signaturePublicUrl(tenantId);
            // Aggiorna le impostazioni con il nuovo URL
            TenantSettingsDto currentSettings = service.loadForTenant(tenantId);
            TenantSettingsDto updatedSettings = new TenantSettingsDto(
                    currentSettings.displayName(),
                    currentSettings.contactEmail(),
                    currentSettings.website(),
                    currentSettings.issuerName(),
                    currentSettings.issuerRole(),
                    currentSettings.logoUrl(),
                    signatureUrl,
                    currentSettings.primaryColor(),
                    currentSettings.defaultTemplateId()
            );
            service.saveForTenant(tenantId, updatedSettings);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("url", signatureUrl);
            response.put("message", "Firma caricata con successo");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Errore durante l'upload: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}