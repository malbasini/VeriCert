package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Stato;
import com.example.vericert.domain.Tenant;
import com.example.vericert.dto.CertificateDto;
import com.example.vericert.dto.CreateReq;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.CertificateService;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.UsageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @GetMapping("/list")
    public Page<CertificateDto> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        Long tenantId = currentTenantId();
        var p = certRepo.findByTenantId(tenantId,
                PageRequest.of(page, size, Sort.by("status").ascending()));
        return p.map(CertificateMapper::toDto);
    }

    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
    @PostMapping()
    public ResponseEntity<?> create(@Valid @RequestBody CreateReq req, BindingResult br) throws IOException {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            fe -> fe.getField(),
                            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message","Validation failed","errors",errors));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        Certificate c = null;
        try {
            Tenant tenant = tenantRepo.findByName(tenantName);
            // controllo piano
            usageService.assertCanIssue(tenant.getId(), tenant.getPlan());
            c = service.issue(req.templateId(), req.vars(), req.ownerName(), req.ownerEmail(), req.courseCode());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(new CertificateApiController.VerificationCreateResponse(
                c.getId().toString(),
                c.getSerial(),
                c.getPdfUrl(),
                c.getSha256(),
                c.getOwnerName(),
                c.getOwnerEmail(),
                c.getIssuedAt().toString()
        ));
    }
    // DTO interno alla risposta
    record VerificationCreateResponse(
            String code,
            String serial,
            String pdfUrl,
            String sha256,
            String ownerName,
            String ownerEmail,
            String issuedAt
    ) { }

    @PostMapping("/{code}/revoke")
    public ResponseEntity<?> revoke(@PathVariable(name = "code") Long code,
                                    @RequestBody Map<String,String> body,
                                    Principal principal
                                    ) {
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
                    certificate.getIssuedAt().toString(),
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
            String issuedAt,
            String revokedReason,
            String revokedAt

    ) {}

    @GetMapping("/codes")
    public ResponseEntity<?> getValidCodes() {
        var codes = certRepo.findAll().stream()
                .filter(z -> z.getStatus() != Stato.REVOKED)
                .filter(z -> Objects.equals(z.getTenant().getId(), currentTenantId()))
                .map(Certificate::getId)
                .toList();
        return ResponseEntity.ok(codes);
    }
}


