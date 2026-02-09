package com.example.vericert.controller;

import com.example.vericert.service.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminHealthController {

    @GetMapping("/admin/health")
    public String healthPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        model.addAttribute("pageTitle", "Health & Log");
        model.addAttribute("active", "health");
        model.addAttribute("currentTenantName", user.getTenantName());
        model.addAttribute("currentUserName", user.getUsername());
        // La pagina farà poi fetch via JS dei dati live
        return "health/health";
    }
}
