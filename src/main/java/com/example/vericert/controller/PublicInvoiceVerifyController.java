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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

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
            , PlanEnforcementService planEnforcementService,
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
        } catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                return ResponseEntity
                        .status(429) // Too Many Requests
                        .body(Map.of("message", "Hai raggiunto il limite di chiamate API per il tuo piano."));
            }
        }
        // 1) carica PDF firmato (originale)
        byte[] signedPdf;
        try {
            signedPdf = certStorage.loadPdfBytes(tenant.getId(), inv.getSerial());
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

    @PostMapping(value = "/{publicCode}/verify-copy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyCopy(@PathVariable String publicCode,
                                        @RequestPart("pdf") MultipartFile pdf) throws IOException {

        // 0) Validazioni base upload
        if (pdf == null || pdf.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Carica un PDF per verificare la copia."));
        }
        String ct = pdf.getContentType();
        if (ct != null && !ct.equalsIgnoreCase("application/pdf")) {
            // NB: alcuni browser mandano ct generico; se vuoi essere più permissivo, togli questo controllo
            return ResponseEntity.status(400).body(Map.of("error", "Il file caricato non sembra un PDF."));
        }

        // 1) Esegui la stessa verifica “ufficiale” della GET (token, scadenza, revoca, firma PAdES)
        PublicInvoiceVerifyController.VerifiedOriginal verified;
        try {
            verified = verifyOriginalOrThrow(publicCode); // vedi metodo sotto
        } catch (PublicInvoiceVerifyController.ApiError e) {
            return ResponseEntity.status(e.httpStatus).body(Map.of("error", e.message));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Errore interno durante la verifica."));
        }

        // 2) Confronto copia vs originale (hash SHA-256 dei bytes)
        byte[] uploadedBytes = pdf.getBytes();

        String origSha = sha256Hex(verified.signedPdfBytes);
        String copySha = sha256Hex(uploadedBytes);

        if (!Objects.equals(origSha, copySha)) {
            // Originale valido ma copia diversa => manomissione / copia non identica
            return ResponseEntity.status(422).body(Map.of(
                    "error", "La copia caricata NON corrisponde all’originale archiviato (possibile manomissione o conversione).",
                    "status", "COPY_MISMATCH",
                    "signatureOk", true,
                    "signerCn", verified.signerCn,
                    "signedAt", verified.signedAt
            ));
        }

        // 3) Se match: ok
        // Metering: qui decidi la policy. Io incrementerei verifications + apiCalls anche qui,
        // perché è una verifica “reale” e potenzialmente abusabile.
        usageMeterService.incrementVerifications(verified.tenantId);
        usageMeterService.incrementApiCalls(verified.tenantId);

        return ResponseEntity.ok(Map.of(
                "status", "VALID",
                "signatureOk", true,
                "copyChecked", true,
                "copyMatches", true,
                "signerCn", verified.signerCn,
                "signedAt", verified.signedAt
        ));
    }
    private record VerifiedOriginal(long tenantId, byte[] signedPdfBytes, String signerCn, String signedAt) {}

    private static class ApiError extends RuntimeException {
        final int httpStatus;
        final String message;

        ApiError(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
            this.message = message;
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private VerifiedOriginal verifyOriginalOrThrow(String publicCode) throws IOException {
        Invoice inv = invoiceRepo.findByPublicCode(publicCode)
                .orElseThrow(() -> new IllegalArgumentException("Fattura non trovata"));

        Long tenantId = inv.getTenantId();
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        // blocco se piano scaduto / oltre o uguale soglia API
        try {
            planEnforcementService.checkCanCallApi(tenantId);
        } catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                throw new ApiError(429,"Hai raggiunto il limite di chiamate API per il tuo piano.");
            }
        }
        // 1) carica PDF firmato (originale)
        byte[] signedPdf;
        try {
            signedPdf = certStorage.loadPdfBytes(tenant.getId(), inv.getSerial());
        } catch (Exception e) {
            throw new ApiError(500,"Impossibile caricare il documento.");
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
            throw new ApiError(500,"Chiave di firma non configurata.");
        }

        X509Certificate tenantCert;
        try {
            tenantCert = CertificatePemUtils.parseX509(sk.getCertPem());
        } catch (Exception e) {
            throw new ApiError(500,"Certificato di firma non valido.");
        }

        // 3) valida PAdES
        var sig = pdfSigValidationService.validate(signedPdf, tenantCert);
        if (!sig.ok()) {
            throw new ApiError(422,"Documento alterato o firma non valida.");
        }

        // 4) metering (solo se firma OK e stato OK)
        usageMeterService.incrementVerifications(inv.getTenantId());
        usageMeterService.incrementApiCalls(inv.getTenantId());

        return new PublicInvoiceVerifyController.VerifiedOriginal(tenant.getId(), signedPdf, sig.signerCn(), sig.signingTime());
    }
}
