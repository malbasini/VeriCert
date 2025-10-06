package com.example.vericert.controller;


import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController {

    private final TenantRepository tenantRepo;
    private final TemplateRepository templateRepo;

    public HomeController(TenantRepository tenantRepo, TemplateRepository templateRepo) {
        this.tenantRepo = tenantRepo;
        this.templateRepo = templateRepo;
    }
    @GetMapping("/home")
    public String showHomePage()
    {
        return "home";
    }
    @GetMapping("/certificati")
    public String insertCertificate(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        Long tenantId = user.getTenantId();
        Long templateId = templateRepo.findFirstProjectedByTenant_Id(tenantId)
                .map(TemplateRepository.TemplateIdOnly::getId)
                .orElse(null);
        model.addAttribute("tenantName", tenantName);
        model.addAttribute("templateId", templateId);
        // Ritorna la vista
        return "certificates/certificate";
    }
    @GetMapping("/revoke")
    public String revokeCertificate(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        model.addAttribute("tenantName", tenantName);
        // Ritorna la vista
        return "certificates/revoke";
    }
    @GetMapping("/verify")
    public String VerifyCertificate() {
        // Ritorna la vista
        return "verification/result";
    }




}
