package com.example.vericert.controller;


import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        Template template = templateId == null ? null : templateRepo.findById(templateId).orElseThrow();
        assert template != null;
        String userVarSchema = template.getUserVarSchema();
        model.addAttribute("userVarSchema", userVarSchema);
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
        return "verification/resultJson";
    }
    @GetMapping("/api/admin/templates/view")
    public String Template() {
        // Ritorna la vista
        return "admin/templates";
    }
    @GetMapping("/api/admin/templates/create")
    public String CreateTemplate() {
        // Ritorna la vista
        return "admin/create";
    }
    @GetMapping("/api/admin/templates/{id}/update")
    public String updateTemplate(@PathVariable(name="id") Long id, Model model) {
        Template template = templateRepo.findById(id).orElseThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        model.addAttribute("tenantName", tenantName);
        // Ritorna la vista
        model.addAttribute("template", template);
        return "admin/update";
    }

    @GetMapping("/certificate/list")
    public String certificateList(Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        model.addAttribute("tenantName", tenantName);
        return "_list";
    }

    @GetMapping("/admin/page")
    public String admin(Model model) {
        return "admin";
    }




}
