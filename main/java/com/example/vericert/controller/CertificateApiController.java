package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Tenant;
import com.example.vericert.dto.CreateReq;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CertificateService;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.UsageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
//Emissione di un certificato
public class CertificateApiController {

    private final CertificateService service;

    private final UsageService usageService;
    private final TenantRepository tenantRepo;

    public CertificateApiController(CertificateService service,
                                    UsageService usageService,
                                    TenantRepository tenantRepo) {

        this.service = service;
        this.usageService = usageService;
        this.tenantRepo = tenantRepo;
    }

    @PostMapping()
    public ResponseEntity<?> create(@RequestBody CreateReq req) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        Tenant tenant = tenantRepo.findByName(tenantName);
        // controllo piano
        usageService.assertCanIssue(tenant.getId(), tenant.getPlan());
        Certificate c = service.issue(req.templateId(), req.vars(), req.ownerName(), req.ownerEmail(), req.courseCode());
        return ResponseEntity.ok(Map.of("id", c.getId(), "serial", c.getSerial(), "pdfUrl", c.getPdfUrl(), "sha256", c.getSha256()));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable(name = "id") Long id,
                                    @RequestBody Map<String,String> body,
                                    Principal principal) {
        try {
            service.revoke(id,
                    body.getOrDefault("reason",""),
                    principal != null ? principal.getName() : "api");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


}


