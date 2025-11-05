package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.Tenant;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.*;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificates")
//Emissione di un certificato
public class CertificateApiController {

    private final CertificateService service;
    private final UsageService usageService;
    private final TenantRepository tenantRepo;
    private final CertificateRepository certRepo;
    private final TemplateRepository tempRepo;
    private final TemplatePicker templatePicker;
    private final CaptchaValidator captchaValidator;

    public CertificateApiController(CertificateService service,
                                    UsageService usageService,
                                    TenantRepository tenantRepo,
                                    CertificateRepository certRepo,
                                    TemplateRepository tempRepo,
                                    TemplatePicker templatePicker,
                                    CaptchaValidator captchaValidator) {

        this.service = service;
        this.usageService = usageService;
        this.tenantRepo = tenantRepo;
        this.certRepo = certRepo;
        this.tempRepo = tempRepo;
        this.templatePicker = templatePicker;
        this.captchaValidator = captchaValidator;
    }
    @GetMapping("/list")
    public Page<Certificate> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required=false) String q,
                                  @RequestParam(required=false) Stato status,
                                  @PageableDefault(size=10, sort="updatedAt", direction=Sort.Direction.DESC) Pageable pageable) {

        Long tenantId=currentTenantId(); // prendi dal tuo CustomUserDetails o threadlocal
        return service.listForTenant(tenantId, q, status, pageable);
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
    @PostMapping("/new/{ownerName}/{ownerEmail}")
    public ResponseEntity<?> create(
            @PathVariable(name="ownerName") String ownerName,
            @PathVariable(name="ownerEmail") String ownerEmail,
            @RequestParam("g-recaptcha-response") String captchaResponse,
            @Valid @RequestBody Map<String,Object> map,
            BindingResult br) throws IOException {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message","Validation failed","errors",errors));
        }
        boolean isCaptchaValid = captchaValidator.verifyCaptcha(captchaResponse);
        if (!isCaptchaValid) {
            return ResponseEntity.badRequest().body(Map.of("message", "Captcha failed", "errors", "Invalid Captcha"));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        map.put("tenantName", tenantName);
        Long tenantId = currentTenantId();
        Template tpl = templatePicker.getActiveTemplateOrThrow(tenantId);
        Certificate c = null;
        try {
            Optional<Tenant> t = tenantRepo.findByName(tenantName);
            Tenant tenant = t.orElseThrow();
            c = service.issue(tpl.getId(), map, ownerName, ownerEmail,tenant);
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
    @GetMapping("/loadCodes")
    public ResponseEntity<?> getTemplate() {
        var codes = tempRepo.findAll().stream()
                .filter(z -> Objects.equals(z.getTenant().getId(), currentTenantId()))
                .filter(Template::isActive)
                .map(Template::getId)
                .toList();
        return ResponseEntity.ok(codes);
    }
}


