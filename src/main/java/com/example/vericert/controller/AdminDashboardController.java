package com.example.vericert.controller;

import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.dto.TenantUsageStatusDTO;
import com.example.vericert.enumerazioni.Status;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.MembershipRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.service.TenantUsageStatusService;
import com.example.vericert.service.UsageMeterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
public class AdminDashboardController {

    private final TemplateRepository templateRepo;
    private final CertificateRepository certRepo;
    private final MembershipRepository membershipRepo;
    private final TenantRepository tenantRepo;
    private final TenantUsageStatusService tenantUsageStatusService;
    private final PlanEnforcementService planEnforcementService;

    public AdminDashboardController(
            TemplateRepository templateRepo,
            CertificateRepository certRepo,
            MembershipRepository membershipRepo,
            TenantRepository tenantRepo,
            UsageMeterService usageMeterService,
            TenantUsageStatusService tenantUsageStatusService,
            PlanEnforcementService planEnforcementService
    ) {
        this.templateRepo = templateRepo;
        this.certRepo = certRepo;
        this.membershipRepo = membershipRepo;
        this.tenantRepo = tenantRepo;
        this.tenantUsageStatusService = tenantUsageStatusService;
        this.planEnforcementService = planEnforcementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String tenantName = "N/A";
        Long tenantId = null;
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails cud) {
            tenantName = cud.getTenantName();
            tenantId = tenantRepo.findByName(tenantName).get().getId();
        }
        CurrentPlanView planView = planEnforcementService.buildCurrentPlanView(tenantId);
        model.addAttribute("currentPlan", planView);

        long totalTemplates = (tenantId != null)
                ? templateRepo.countByTenantId(tenantId)
                : 0L;

        long totalCertificates = (tenantId != null)
                ? planView.getUsedCerts()
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
        model.addAttribute("title", "Come funziona Vercert | Certificati digitali verificabili per aziende");
        model.addAttribute("description", "Scopri come funziona Vercert: crea, gestisci e verifica certificati digitali con QR code in modo sicuro per aziende in Italia.");
        return "dashboard";
    }
    @GetMapping("/usage_meter")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String usage(Model model) {
        // stato consumo di tutti i tenant oggi, con limiti e semaforo storage
        List<TenantUsageStatusDTO> todayStatus = tenantUsageStatusService.buildTodayStatusForAllTenants();
        model.addAttribute("todayStatus", todayStatus);
        return "usage/usage";
    }
}
