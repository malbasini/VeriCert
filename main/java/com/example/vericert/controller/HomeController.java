package com.example.vericert.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    public HomeController(){
    }

    @GetMapping()
    public String showHomePage(Model model) {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm() {return "security/login";}

    @GetMapping("/stripe")
    public String showStripeForm(Model model) {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "stripe/redirect";
    }

    @GetMapping("/paypal")
    public String showPaypalForm(Model model) {
        Long tenantId = currentTenantId();
        model.addAttribute("tenantId", tenantId);
        return "paypal/form";
    }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
