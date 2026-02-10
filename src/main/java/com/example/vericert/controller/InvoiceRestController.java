package com.example.vericert.controller;

import com.example.vericert.domain.Invoice;
import com.example.vericert.dto.InvoiceLineResp;
import com.example.vericert.dto.InvoiceResp;
import com.example.vericert.dto.UpsertInvoiceReq;
import com.example.vericert.repo.InvoiceRepository;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequestMapping("/api/invoices")
public class InvoiceRestController {

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepo;

    public InvoiceRestController(InvoiceService invoiceService, InvoiceRepository invoiceRepo) {
        this.invoiceService = invoiceService;
        this.invoiceRepo = invoiceRepo;
    }

    @GetMapping
    public List<InvoiceResp> list() {
        Long tenantId = currentTenantId();
        var invoices = invoiceRepo.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::toResp).toList();
        return invoices;
    }

    @GetMapping("/{id}")
    public InvoiceResp get(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        Invoice inv = invoiceService.getOwned(tenantId, id);
        return toResp(inv);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid UpsertInvoiceReq req, BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        Long tenantId = currentTenantId();
        try {
            return ResponseEntity.ok(toResp(invoiceService.createDraft(tenantId, req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpsertInvoiceReq req,BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest().body(Map.of("message", "Validation failed", "errors", errors));
        }
        Long tenantId = currentTenantId();
        try {
            return ResponseEntity.ok(toResp(invoiceService.updateDraft(tenantId, id, req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }
    @PostMapping("/{id}/issue")
    public ResponseEntity<?> issue(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        try {
            return ResponseEntity.ok(toResp(invoiceService.issue(tenantId, id)));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        Invoice inv = invoiceRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice non trovata"));

        if (inv.getPdfBlob() == null || inv.getPdfBlob().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String filename = (inv.getNumberDisplay() != null ? inv.getNumberDisplay() : inv.getPublicCode());
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(inv.getPdfBlob());
    }

    private InvoiceResp toResp(Invoice inv) {
        var lines = inv.getLines() == null ? List.<InvoiceLineResp>of() :
                inv.getLines().stream().map(l -> new InvoiceLineResp(
                        l.getId(),
                        l.getDescription(),
                        l.getQty(),
                        l.getUnitPriceMinor(),
                        l.getVatRate(),
                        l.getNetMinor(),
                        l.getVatMinor(),
                        l.getGrossMinor()
                )).toList();

        return new InvoiceResp(
                inv.getId(),
                inv.getTenantId(),
                inv.getPublicCode(),
                inv.getStatus().name(),
                inv.getIssueYear(),
                inv.getNumberSeq(),
                inv.getNumberDisplay(),
                inv.getIssuedAt().toString(),
                inv.getCustomerName(),
                inv.getCustomerVat(),
                inv.getDescription(),
                inv.getCustomerEmail(),
                inv.getCustomerAddressLine1(),
                inv.getCustomerAddressLine2(),
                inv.getCustomerCity(),
                inv.getCustomerProvince(),
                inv.getCustomerPostalCode(),
                inv.getCustomerCountry(),
                inv.getCustomerPec(),
                inv.getCustomerSdi(),
                inv.getCurrency(),
                inv.getVatRate(),
                inv.getNetTotalMinor(),
                inv.getVatTotalMinor(),
                inv.getGrossTotalMinor(),
                inv.getTemplateId(),
                inv.getSerial(),
                lines
        );
    }

    private Long currentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
