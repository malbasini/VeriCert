package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.Tenant;
import com.example.vericert.dto.VerificationOutcome;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.PlanEnforcementService;
import com.example.vericert.service.QrVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v")
public class PublicVerificationController {

    private final VerificationTokenRepository verificationRepo;
    private final CertificateRepository certificateRepo;
    private final QrVerificationService service;
    private final PlanEnforcementService planEnforcementService;

    public PublicVerificationController(VerificationTokenRepository verificationRepo,
                                        CertificateRepository certificateRepo,
                                        QrVerificationService service,
                                        PlanEnforcementService planEnforcementService) {
        this.verificationRepo = verificationRepo;
        this.certificateRepo = certificateRepo;
        this.service = service;
        this.planEnforcementService = planEnforcementService;
    }

    /**
     * Verifica pubblica del certificato tramite codice QR
     * Esempio: GET /v/ABC123XYZ
     */
    @GetMapping(value="/{code}")
    public ResponseEntity<?> verifyCertificate(@PathVariable("code") String code) throws IOException {

        Optional<VerificationToken> tokenOpt = verificationRepo.findByCode(code);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VerificationToken token = tokenOpt.get();
        Long certId = token.getCertificateId();
        Certificate certificate = certificateRepo.getById(certId);
        Tenant tenant = certificate.getTenant();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(certificate.getIssuedAt());
        // blocco se piano scaduto / oltre o uguale soglia API
        try {
            planEnforcementService.checkCanCallApi(tenant.getId());
        }
        catch (PlanLimitExceededException e) {
            if (e.getType() == PlanViolationType.API_QUOTA_EXCEEDED) {
                return ResponseEntity
                        .status(429) // Too Many Requests
                        .body(Map.of("error", "Hai raggiunto il limite di chiamate API per il tuo piano."));
            }
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
        if (certificate.getStatus() == Stato.REVOKED) {
            return ResponseEntity.status(403).body(Map.of("error", "Certificato revocato."));
        }
        try
        {
            VerificationOutcome out = service.verify(tenant.getId(), token.getCompactJws(), QrVerificationService.Source.API,code);
            if (!out.ok()){
                return ResponseEntity.status(422).body(Map.of("error", "Verifica fallita."));
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
        // Puoi restituire un DTO con i dati del certificato
        return ResponseEntity.ok(new VerificationResponse(
                token.getCode(),
                certificate.getOwnerName(),
                certificate.getOwnerEmail(),
                formatted
        ));
    }

    // DTO interno alla risposta
    public record VerificationResponse(
            String code,
            String ownerName,
            String ownerEmail,
            String issueDate
    ) {}

}