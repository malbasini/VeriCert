package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.Tenant;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.SigningKeyRepository;
import com.example.vericert.repo.TenantSigningKeyRepository;
import com.example.vericert.repo.VerificationTokenRepository;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/v")
public class PublicVerificationController {

    private final VerificationTokenRepository verificationRepo;
    private final CertificateRepository certificateRepo;
    private final PlanEnforcementService planEnforcementService;
    private final UsageMeterService usageMeterService;
    private final CertificateStorageService certStorage;
    private final SigningKeyRepository signingKeyRepo;
    private final TenantSigningKeyRepository tenantSigningKeyRepo;
    private final PdfSignatureValidationService pdfSigValidationService;

    public PublicVerificationController(
            VerificationTokenRepository verificationRepo,
            CertificateRepository certificateRepo,
            PlanEnforcementService planEnforcementService,
            UsageMeterService usageMeterService,
            CertificateStorageService certStorage,
            SigningKeyRepository signingKeyRepo,
            TenantSigningKeyRepository tenantSigningKeyRepo,
            PdfSignatureValidationService pdfSigValidationService
    ) {
        this.verificationRepo = verificationRepo;
        this.certificateRepo = certificateRepo;
        this.planEnforcementService = planEnforcementService;
        this.usageMeterService = usageMeterService;
        this.certStorage = certStorage;
        this.signingKeyRepo = signingKeyRepo;
        this.tenantSigningKeyRepo = tenantSigningKeyRepo;
        this.pdfSigValidationService = pdfSigValidationService;
    }

    @GetMapping(value="/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verify(@PathVariable String code) throws IOException {

        var tokenOpt = verificationRepo.findByCode(code);
        if (tokenOpt.isEmpty()) return ResponseEntity.notFound().build();
        var token = tokenOpt.get();

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(404).body(Map.of("error", "Certificato scaduto."));
        }

        Certificate cert = null;
        Tenant tenant = null;
        try {
            cert = certificateRepo.getById(token.getCertificateId());
            tenant = cert.getTenant();

            planEnforcementService.checkCanCallApi(tenant.getId());

        } catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                return ResponseEntity.status(429).body(Map.of("message", "Hai raggiunto il limite di chiamate API per il tuo piano."));
            }
        }

        if (cert.getStatus() == Stato.REVOKED) {
            return ResponseEntity.status(403).body(Map.of("error", "Certificato revocato."));
        }

        // 1) carica PDF firmato (originale)
        byte[] signedPdf;
        try {
            signedPdf = certStorage.loadPdfBytes(tenant.getId(), cert.getSerial());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Impossibile caricare il documento."));
        }

        // 2) scegli il KID corretto per verificare (rotazione-safe)
        String kidToUse = cert.getKid(); // <-- colonna nuova
        SigningKeyEntity sk = null;

        if (kidToUse != null && !kidToUse.isBlank()) {
            sk = signingKeyRepo.findById(kidToUse).orElse(null); // anche RETIRED va bene per verify
        } else {
            // fallback: se certificati storici non hanno signingKid, usa la ACTIVE del tenant
            sk = tenantSigningKeyRepo.findActiveSigningKeyByTenant(tenant.getId()).orElse(null);
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
        usageMeterService.incrementVerifications(tenant.getId());
        usageMeterService.incrementApiCalls(tenant.getId());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
        String issuedAt = fmt.format(cert.getIssuedAt());

        return ResponseEntity.ok(new VerificationResponse(
                token.getCode(),
                cert.getSerial(),
                cert.getOwnerName(),
                cert.getOwnerEmail(),
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

    @PostMapping(value="/{code}/verify-copy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyCopy(@PathVariable String code,
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
        VerifiedOriginal verified;
        try {
            verified = verifyOriginalOrThrow(code); // vedi metodo sotto
        } catch (ApiError e) {
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

    private static class ApiError extends RuntimeException {
        final int httpStatus;
        final String message;
        ApiError(int httpStatus, String message) { super(message); this.httpStatus = httpStatus; this.message = message; }
    }

    private record VerifiedOriginal(long tenantId, byte[] signedPdfBytes, String signerCn, String signedAt) {}

    private VerifiedOriginal verifyOriginalOrThrow(String code) throws IOException {

        var tokenOpt = verificationRepo.findByCode(code);
        if (tokenOpt.isEmpty()) throw new ApiError(404, "Codice di verifica non valido.");

        var token = tokenOpt.get();

        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiError(404, "Certificato scaduto.");
        }

        Certificate cert;
        Tenant tenant;
        try {
            cert = certificateRepo.getById(token.getCertificateId());
            tenant = cert.getTenant();
            planEnforcementService.checkCanCallApi(tenant.getId());
        } catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                throw new ApiError(429, "Hai raggiunto il limite di chiamate API per il tuo piano.");
            }
            throw e;
        }

        if (cert.getStatus() == Stato.REVOKED) throw new ApiError(403, "Certificato revocato.");

        // carica PDF originale firmato
        byte[] signedPdf;
        try {
            signedPdf = certStorage.loadPdfBytes(tenant.getId(), cert.getSerial());
        } catch (Exception e) {
            throw new ApiError(500, "Impossibile caricare il documento.");
        }

        // scegli certificato X509 corretto (rotazione-safe)
        String kidToUse = cert.getKid();
        SigningKeyEntity sk;
        if (kidToUse != null && !kidToUse.isBlank()) {
            sk = signingKeyRepo.findById(kidToUse).orElse(null);
        } else {
            sk = tenantSigningKeyRepo.findActiveSigningKeyByTenant(tenant.getId()).orElse(null);
        }

        if (sk == null || sk.getCertPem() == null || sk.getCertPem().isBlank()) {
            throw new ApiError(500, "Chiave di firma non configurata.");
        }

        X509Certificate tenantCert;
        try {
            tenantCert = CertificatePemUtils.parseX509(sk.getCertPem());
        } catch (Exception e) {
            throw new ApiError(500, "Certificato di firma non valido.");
        }

        var sig = pdfSigValidationService.validate(signedPdf, tenantCert);
        if (!sig.ok()) throw new ApiError(422, "Documento alterato o firma non valida.");

        return new VerifiedOriginal(tenant.getId(), signedPdf, sig.signerCn(), sig.signingTime());
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


}