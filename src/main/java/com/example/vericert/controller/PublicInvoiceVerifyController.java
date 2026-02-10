package com.example.vericert.controller;

import com.example.vericert.domain.Invoice;
import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.Tenant;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.InvoiceRepository;
import com.example.vericert.repo.SigningKeyRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSigningKeyRepository;
import com.example.vericert.service.CertificateStorageService;
import com.example.vericert.service.PdfSignatureValidationService;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.service.UsageMeterService;
import com.example.vericert.util.CertificatePemUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/v/invoices")
public class PublicInvoiceVerifyController {

    private final InvoiceRepository invoiceRepo;
    private final PlanEnforcementService planEnforcementService;
    private final UsageMeterService usageMeterService;
    private final SigningKeyRepository signingKeyRepo;
    private final TenantSigningKeyRepository tenantSigningKeyRepo;
    private final PdfSignatureValidationService pdfSigValidationService;
    private final CertificateStorageService certStorage;
    private final TenantRepository tenantRepo;


    public PublicInvoiceVerifyController(InvoiceRepository invoiceRepo
                                        ,PlanEnforcementService planEnforcementService,
                                         UsageMeterService usageMeterService,
                                         SigningKeyRepository signingKeyRepo,
                                         TenantSigningKeyRepository tenantSigningKeyRepo,
                                         PdfSignatureValidationService pdfSigValidationService,
                                         CertificateStorageService certStorage,
                                         TenantRepository tenantRepo) {


        this.invoiceRepo = invoiceRepo;
        this.planEnforcementService = planEnforcementService;
        this.usageMeterService = usageMeterService;
        this.signingKeyRepo = signingKeyRepo;
        this.tenantSigningKeyRepo = tenantSigningKeyRepo;
        this.pdfSigValidationService = pdfSigValidationService;
        this.certStorage = certStorage;
        this.tenantRepo = tenantRepo;

    }

    @GetMapping("/{publicCode}")
    public ResponseEntity<?> verify(@PathVariable String publicCode) {

        Invoice inv = invoiceRepo.findByPublicCode(publicCode)
                .orElseThrow(() -> new IllegalArgumentException("Fattura non trovata"));

        Long tenantId = inv.getTenantId();
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
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
        // 1) carica PDF firmato (originale)
        byte[] signedPdf;
        try {
            signedPdf = inv.getPdfBlob();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Impossibile caricare il documento."));
        }

        // 2) scegli il KID corretto per verificare (rotazione-safe)
        String kidToUse = inv.getKid(); // <-- colonna nuova
        SigningKeyEntity sk = null;

        if (kidToUse != null && !kidToUse.isBlank()) {
            sk = signingKeyRepo.findById(kidToUse).orElse(null); // anche RETIRED va bene per verify
        } else {
            // fallback: se certificati storici non hanno signingKid, usa la ACTIVE del tenant
            sk = tenantSigningKeyRepo.findActiveSigningKeyByTenant(inv.getTenantId()).orElse(null);
        }

        if (sk == null || sk.getCertPem() == null || sk.getCertPem().isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "Chiave di firma non configurata."));
        }

        X509Certificate tenantCert;
        try {
            tenantCert = CertificatePemUtils.parseX509(sk.getCertPem());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Certificato di firma non valido."));
        }

        // 3) valida PAdES
        var sig = pdfSigValidationService.validate(signedPdf, tenantCert);
        if (!sig.ok()) {
            return ResponseEntity.status(422).body(Map.of("error", "Documento alterato o firma non valida."));
        }

        // 4) metering (solo se firma OK e stato OK)
        usageMeterService.incrementVerifications(inv.getTenantId());
        usageMeterService.incrementApiCalls(inv.getTenantId());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
        String issuedAt = fmt.format(inv.getIssuedAt());

        return ResponseEntity.ok(new PublicVerificationController.VerificationResponse(
                inv.getPublicCode(),
                inv.getSerial(),
                inv.getCustomerName(),
                inv.getCustomerEmail(),
                issuedAt,
                tenant.getName(),
                true,
                sig.signerCn(),
                sig.signingTime(),
                "VALID"
        ));
    }
    // DTO interno alla risposta
    public record VerificationResponse(
            String code,
            String serial,
            String ownerName,
            String ownerEmail,
            String issueDate,
            String issuerName,
            boolean signatureOk,
            String signerCn,
            String signedAt,
            String status
    ) {}
}
