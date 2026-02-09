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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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

}