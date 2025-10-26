package com.example.vericert.controller;

import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.MembershipRepository;
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
@RequestMapping("/admin")
public class AdminDashboardController {

    private final TemplateRepository templateRepo;
    private final CertificateRepository certRepo;
    private final MembershipRepository membershipRepo;
    private final TenantRepository tenantRepo;

    public AdminDashboardController(
            TemplateRepository templateRepo,
            CertificateRepository certRepo,
            MembershipRepository membershipRepo,
            TenantRepository tenantRepo
    ) {
        this.templateRepo = templateRepo;
        this.certRepo = certRepo;
        this.membershipRepo = membershipRepo;
        this.tenantRepo = tenantRepo;
    }

    @GetMapping
    public String dashboard(Model model) {

        // recupero tenant corrente (tu hai già CustomUserDetails con tenantName)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String tenantName = "N/A";
        Long tenantId = null;

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            tenantName = cud.getTenantName();
            tenantId = tenantRepo.findByName(tenantName).get().getId();
        }

        long totalTemplates   = (tenantId != null)
                ? templateRepo.countByTenantId(tenantId)
                : 0L;

        long totalCertificates = (tenantId != null)
                ? certRepo.countByTenantId(tenantId)
                : 0L;

        long totalUsers = (tenantId != null)
                ? membershipRepo.countActiveUsersByTenant(tenantId, Status.ACTIVE)
                : 0L;

        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("active", "dashboard");

        model.addAttribute("currentTenant", tenantName);
        model.addAttribute("totalTemplates", totalTemplates);
        model.addAttribute("totalCertificates", totalCertificates);
        model.addAttribute("totalUsers", totalUsers);

        return "dashboard";
    }
}
