package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.TemplatePicker;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/certificates")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
public class CertificateAdminController {

    private final CertificateRepository repo;
    private final TemplatePicker templatePicker;
    private final TenantRepository tenantRepo;
    private final VerificationTokenRepository tokenRepo;

    public CertificateAdminController(CertificateRepository repo,
                                      TemplatePicker templatePicker,
                                      TenantRepository tenantRepo,
                                      VerificationTokenRepository tokenRepo) {
        this.repo = repo;
        this.templatePicker = templatePicker;
        this.tenantRepo = tenantRepo;
        this.tokenRepo = tokenRepo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Stato status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "issuedAt,desc") String sort,
            Model model
    ) {
        Long tenantId = currentTenantId(); // se non l’hai, ricavalo da repo col tenantName

        Sort sortObj = Sort.by(
                sort.contains(",")
                        ? Sort.Order.by(sort.split(",")[0])
                        .with("desc".equalsIgnoreCase(sort.split(",")[1]) ? Sort.Direction.DESC : Sort.Direction.ASC)
                        : Sort.Order.desc("issuedAt")
        );
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<Certificate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (StringUtils.hasText(q)) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("serial")), like),
                                cb.like(cb.lower(root.get("ownerName")), like),
                                cb.like(cb.lower(root.get("ownerEmail")), like)
                        )
                );
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            ZoneId zone = ZoneId.systemDefault();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issuedAt"),
                        from.atStartOfDay(zone).toInstant()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("issuedAt"),
                        to.plusDays(1).atStartOfDay(zone).toInstant()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Certificate> result = repo.findAll(spec, pageable);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("sort", sort);
        model.addAttribute("tenantId", tenantId);
        return "certificates/list";
    }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
    @GetMapping("/new")
    public String newCertificate(Model model) {
        Long tenantId = currentTenantId();
        Template tpl = templatePicker.getActiveTemplateOrThrow(tenantId);
        model.addAttribute("template", tpl);
        model.addAttribute("variablesUserJson", tpl.getUserVarSchema());
        model.addAttribute("pageTitle", "Nuovo certificato");
        model.addAttribute("active", "certificates");
        return "certificates/new";
    }
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER','VIEWER')")
    public String Detail(@PathVariable Long id ,
                         Model model) {
        Long tenantId = currentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        Certificate certificate = repo.findById(id).orElseThrow();
        VerificationToken token = tokenRepo.findByCertificateId(certificate.getId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(token.getExpiresAt());
        Template tpl = templatePicker.getActiveTemplateOrThrow(tenantId);
        model.addAttribute("currentTenant", tenant.getName());
        model.addAttribute("certificate", certificate);
        model.addAttribute("variablesDataJson", certificate.getUserVarsJson());
        model.addAttribute("variablesUserJson", tpl.getUserVarSchema());
        model.addAttribute("templateName", tpl.getName());
        model.addAttribute("pageTitle", "Dettaglio certificato");
        model.addAttribute("active", "certificates");
        model.addAttribute("expiredAt",formatted);
        return "certificates/detail";
    }

    @GetMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
    public String revoke(@PathVariable Long id , Model model) {
        Long tenantId = currentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        Certificate certificate = repo.findById(id).orElseThrow();
        Template tpl = templatePicker.getActiveTemplateOrThrow(tenantId);
        model.addAttribute("currentTenant", tenant.getName());
        model.addAttribute("certificate", certificate);
        model.addAttribute("templateName", tpl.getName());
        model.addAttribute("pageTitle", "Revoca certificato");
        model.addAttribute("active", "certificates");
        return "certificates/revoke";
    }

}
