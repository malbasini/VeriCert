package com.example.vericert.controller;

import com.example.vericert.dto.TenantProfileForm;
import com.example.vericert.service.TenantProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TenantProfileController {

    private final TenantProfileService service;

    public TenantProfileController(TenantProfileService service) {
        this.service = service;
    }

    @PostMapping("/settings/billing-profile")
    public String saveBillingProfile(@Valid @ModelAttribute("billingProfile") TenantProfileForm form,
                                     BindingResult br,
                                     RedirectAttributes ra,
                                     Model model) {


        Long tenantId = currentTenantId(); // TODO tuo metodo

        if (br.hasErrors()) {
            // ritorni alla settings con tab billing aperta
            model.addAttribute("activeTab", "billing");
            return "settings/form"; // la tua view impostazioni
        }

        service.update(tenantId, form);
        ra.addFlashAttribute("toastSuccess", "Dati di fatturazione salvati ✅");
        return "redirect:/settings/form?tab=billing";
    }

    // Utility: in GET /settings ricordati di settare billingProfile:
    // model.addAttribute("billingProfile", TenantProfileService.toForm(service.getOrCreate(tenantId)));

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
