package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Template;
import com.example.vericert.domain.Tenant;
import com.example.vericert.dto.CertificateDto;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.*;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/certificates")
//Emissione di un certificato
@PreAuthorize("hasAnyRole('ADMIN','ISSUER','MANAGER','VIEWER')")
public class CertificateApiController {

    private final CertificateService service;
    private final TenantRepository tenantRepo;
    private final CertificateRepository certRepo;
    private final TemplateRepository tempRepo;
    private final TemplatePicker templatePicker;
    private final CaptchaValidator captchaValidator;
    private final UsageMeterService usageMeterService;
    private final VerificationTokenRepository tokRepo;

    public CertificateApiController(CertificateService service,
                                    TenantRepository tenantRepo,
                                    CertificateRepository certRepo,
                                    TemplateRepository tempRepo,
                                    TemplatePicker templatePicker,
                                    CaptchaValidator captchaValidator,
                                    UsageMeterService usageMeterService,
                                    VerificationTokenRepository tokRepo) {

        this.service = service;
        this.tenantRepo = tenantRepo;
        this.certRepo = certRepo;
        this.tempRepo = tempRepo;
        this.templatePicker = templatePicker;
        this.captchaValidator = captchaValidator;
        this.usageMeterService = usageMeterService;
        this.tokRepo = tokRepo;
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
    @PostMapping("/new/{ownerName}/{ownerEmail}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
    public ResponseEntity<?> create(
            @PathVariable String ownerName,
            @PathVariable String ownerEmail,
            @RequestParam String recaptcha,
            @Valid @RequestBody CertificateDto rec,
            BindingResult br) throws IOException {

        try {
            int hours = Integer.parseInt(rec.hours());
        } catch (NumberFormatException e) {

            br.addError(new FieldError("hours","hours","Il numero ore deve essere un numero intero"));
        }
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        if (!captchaValidator.verifyCaptcha(recaptcha)) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("errors", Map.of("captcha", List.of("Captcha non valido"))));
        }
        Certificate c = null;
        try
        {
        Map<String,Object> map = toMap(rec);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        map.put("tenantName", tenantName);
        Long tenantId = currentTenantId();
        Template tpl = templatePicker.getActiveTemplateOrThrow(tenantId);
        Optional<Tenant> t = tenantRepo.findByName(tenantName);
        Tenant tenant = t.orElseThrow();
            c = service.issue(tpl.getId(), map, ownerName, ownerEmail,tenant);
        }
        catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", e.getMessage()));
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(c.getIssuedAt());
        return ResponseEntity.ok(Map.of("message","success","code",c.getId(),"issuedAt",formatted));
    }
    @PostMapping("/{code}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
    public ResponseEntity<?> revoke(@PathVariable(name = "code") Long code,
                                    @RequestBody Map<String,String> body,
                                    Principal principal
                                    ) {
        Certificate certificate = certRepo.findById(code).orElseThrow();
        if (certificate.getStatus() == Stato.REVOKED) {
            return ResponseEntity.status(410) // Gone
                    .body(Map.of("message","❌ Certificato revocato"));
        }
        try {
            service.revoke(code, body.getOrDefault("reason",""), principal != null ? principal.getName() : "api");
            // Puoi restituire un DTO con i dati del certificato
        } catch (Exception e) {
            return ResponseEntity.status(413)
                    .body(Map.of("message", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message","success"));
    }
    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ISSUER')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try
        {
            //CANCELLO IL FILE PDF
            Certificate c = certRepo.findById(id).orElseThrow();
            Long tenantId = currentTenantId();
            String file = c.getSerial();
            Path p = null;
            try {
                p = Paths.get("storage/", tenantId.toString(), file + ".pdf");
            }
            catch (Exception e) {
                return ResponseEntity.status(410)
                        .body(Map.of("message", e.getMessage()));
            }
            assert p != null;
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            usageMeterService.decrementStorage(currentTenantId(),Files.size(p));
            Files.delete(p);
            service.deleteCertificate(id);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("message","success"));
    }

    public static Map<Object, String> toValueNameMap(Object record) {
        if (!record.getClass().isRecord()) {
            throw new IllegalArgumentException("Not a record");
        }
        var map = new LinkedHashMap<Object, String>();
        for (var c : record.getClass().getRecordComponents()) {
            try {
                Object value = c.getAccessor().invoke(record);
                map.put(value, c.getName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }
    public static Map<String, Object> toMap(Object record) {
        if (!record.getClass().isRecord()) {
            throw new IllegalArgumentException("Not a record");
        }

        var map = new LinkedHashMap<String, Object>();
        for (var c : record.getClass().getRecordComponents()) {
            try {
                Object value = c.getAccessor().invoke(record);
                map.put(c.getName(), value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return map;
    }



}


