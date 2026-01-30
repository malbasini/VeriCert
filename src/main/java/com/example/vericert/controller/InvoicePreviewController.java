package com.example.vericert.controller;

import com.example.vericert.domain.Template;
import com.example.vericert.dto.InvoicePreviewReq;
import com.example.vericert.repo.TemplateRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.InvoicePreviewService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/invoices/templates")
public class InvoicePreviewController {

    private final TemplateRepository templateRepo;
    private final InvoicePreviewService previewService;

    public InvoicePreviewController(TemplateRepository templateRepo, InvoicePreviewService previewService) {
        this.templateRepo = templateRepo;
        this.previewService = previewService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(value="/preview/html", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> previewHtml(@Valid @RequestBody InvoicePreviewReq req, BindingResult br)
    {
        br = validate(br,req);
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        Long tenantId = currentTenantId();
        Template t = templateRepo.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Template non trovato"));
        try {
            String html = previewService.renderHtml(tenantId, t, req);
            return ResponseEntity.ok(html);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping(value="/preview/pdf", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> previewPdf(@Valid @RequestBody InvoicePreviewReq req,BindingResult br) {
        br = validate(br,req);
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        Long tenantId = currentTenantId();
        Template t = templateRepo.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Template non trovato"));

        byte[] pdf = null;
        try {
            pdf = previewService.renderPdf(tenantId, t, req);
        } catch (Exception e) {
           return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview-invoice.pdf\"")
                .body(pdf);
    }

    private BindingResult validate(BindingResult br,InvoicePreviewReq req) {
        if (req.header().get("customerAddressLine1") == null || req.header().get("customerAddressLine1").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerAddressLine1", "L'indirizzo del cliente è obbligatorio"));
        }
        if (req.header().get("customerEmail") == null || req.header().get("customerEmail").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerEmail", "L'email del cliente è obbligatoria"));
        }
        if(req.header().get("customerCity") == null || req.header().get("customerCity").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerCity", "Città obbligatoria"));
        }
        if(req.header().get("customerPostalCode")==null || req.header().get("customerPostalCode").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerPostalCode", "CAP Obbligatorio"));
        }
        if(req.header().get("customerProvince")==null || req.header().get("customerProvince").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerProvince", "Provincia obbligatoria"));
        }
        if(req.header().get("customerName")==null || req.header().get("customerName").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerName", "Nome cliente obbligatorio"));
        }
        if(req.header().get("customerVat")==null || req.header().get("customerVat").toString().isEmpty()) {
            br.addError(new FieldError("header", "customerVat", "Codice fiscale/P.IVA obbligatorio"));
        }
        return br;
    }

    private Long currentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
