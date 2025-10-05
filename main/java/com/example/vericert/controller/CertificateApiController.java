package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Stato;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.dto.CreateReq;
import com.example.vericert.repo.CertificateRepository;
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
import java.io.ObjectInputFilter;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
//Emissione di un certificato
public class CertificateApiController {

    private final CertificateService service;

    private final UsageService usageService;
    private final TenantRepository tenantRepo;
    private final CertificateRepository certRepo;


    public CertificateApiController(CertificateService service,
                                    UsageService usageService,
                                    TenantRepository tenantRepo,
                                    CertificateRepository certRepo) {

        this.service = service;
        this.usageService = usageService;
        this.tenantRepo = tenantRepo;
        this.certRepo = certRepo;
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

    @PostMapping("/{code}/revoke")
    public ResponseEntity<?> revoke(@PathVariable(name = "code") Long code,
                                    @RequestBody Map<String,String> body,
                                    Principal principal) {

        Certificate certificate = certRepo.findById(code).orElseThrow();
        if (certificate.getStatus() == Stato.REVOKED) {
            return ResponseEntity.status(410) // Gone
                    .body("❌ Certificato revocato");
        }
        try {
            service.revoke(code,
                    body.getOrDefault("reason",""),
                    principal != null ? principal.getName() : "api");
            // Puoi restituire un DTO con i dati del certificato
            return ResponseEntity.ok(new CertificateApiController.VerificationResponse(
                    certificate.getId().toString(),
                    certificate.getOwnerName(),
                    certificate.getOwnerEmail(),
                    certificate.getCourseCode(),
                    certificate.getRevokedReason(),
                    certificate.getRevokedAt().toString()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }

    }
    // DTO interno alla risposta
    record VerificationResponse(
            String code,
            String ownerName,
            String ownerEmail,
            String courseCode,
            String revokedReason,
            String revokedAt
    ) {}

    @GetMapping("/codes")
    public ResponseEntity<?> getValidCodes() {
        var codes = certRepo.findAll().stream()
                .filter(z -> z.getStatus() != Stato.REVOKED)
                .map(Certificate::getId)
                .toList();
        return ResponseEntity.ok(codes);
    }
}


