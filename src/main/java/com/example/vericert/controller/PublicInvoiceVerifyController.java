package com.example.vericert.controller;

import com.example.vericert.domain.Invoice;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.InvoiceRepository;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.service.UsageMeterService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/v/invoices")
public class PublicInvoiceVerifyController {

    private final InvoiceRepository invoiceRepo;
    private final PlanEnforcementService planEnforcementService;
    private final UsageMeterService usageMeterService;

    public PublicInvoiceVerifyController(InvoiceRepository invoiceRepo
                                        ,PlanEnforcementService planEnforcementService,
                                         UsageMeterService usageMeterService) {
        this.invoiceRepo = invoiceRepo;
        this.planEnforcementService = planEnforcementService;
        this.usageMeterService = usageMeterService;

    }

    @GetMapping("/{publicCode}")
    public ResponseEntity<?> verify(@PathVariable String publicCode) {

        Invoice inv = invoiceRepo.findByPublicCode(publicCode)
                .orElseThrow(() -> new IllegalArgumentException("Fattura non trovata"));

        Long tenantId = inv.getTenantId();
        // blocco se piano scaduto / oltre o uguale soglia API
        try {
            planEnforcementService.checkCanCallApi(tenantId);
        }
        catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                return ResponseEntity
                        .status(429) // Too Many Requests
                        .body(Map.of("message", "Hai raggiunto il limite di chiamate API per il tuo piano."));
            }
        }
        boolean hasPdf = inv.getPdfBlob() != null && inv.getPdfBlob().length > 0;
        String computed = hasPdf ? sha256Hex(inv.getPdfBlob()) : null;
        InvoiceVerificationDto response = getInvoiceVerificationDto(hasPdf, inv, computed);

        usageMeterService.incrementApiCalls(tenantId);
        return ResponseEntity.ok(response);
    }

    private static @NonNull InvoiceVerificationDto getInvoiceVerificationDto(boolean hasPdf, Invoice inv, String computed) {
        boolean integrityOk = hasPdf && inv.getPdfSha256() != null && inv.getPdfSha256().equalsIgnoreCase(computed);

        InvoiceVerificationDto response = new InvoiceVerificationDto(
                inv.getPublicCode(),
                inv.getStatus().name(),
                inv.getInvoiceCode(),
                inv.getIssuedAt(),
                inv.getCustomerName(),
                inv.getCustomerEmail(),
                inv.getVatRate(),
                inv.getNetTotalMinor(),
                inv.getVatTotalMinor(),
                inv.getGrossTotalMinor(),
                integrityOk
        );
        return response;
    }

    public record InvoiceVerificationDto(
            String publicCode,
            String status,
            String number,
            Instant issuedAt,
            String customerName,
            String customerEmail,
            Integer vatRate,
            Long netTotalMinor,
            Long vatTotalMinor,
            Long grossTotalMinor,
            boolean integrityOk
    ) {}

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
