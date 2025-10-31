package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.example.vericert.service.QrVerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping()
public class PublicVerificationController {

    private final VerificationTokenRepository verificationRepo;
    private final CertificateRepository certificateRepo;
    private final QrVerificationService service;

    public PublicVerificationController(VerificationTokenRepository verificationRepo,
                                        CertificateRepository certificateRepo,
                                        QrVerificationService service) {
        this.verificationRepo = verificationRepo;
        this.certificateRepo = certificateRepo;
        this.service = service;
    }

    /**
     * Verifica pubblica del certificato tramite codice QR
     * Esempio: GET /v/ABC123XYZ
     */
    @GetMapping(value="/v/{code}", produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyCertificate(@PathVariable("code") String code) throws IOException {
        Optional<VerificationToken> tokenOpt = verificationRepo.findByCode(code);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VerificationToken token = tokenOpt.get();
        Long certId = token.getCertificateId();
        Certificate certificate = certificateRepo.getById(certId);

        if (certificate.getStatus() == Stato.REVOKED) {
            return ResponseEntity.status(410) // Gone
                    .body("❌ Certificato revocato");
        }
        service.verify(certificate.getTenant().getId(),code, QrVerificationService.Source.API);
        // Puoi restituire un DTO con i dati del certificato
        return ResponseEntity.ok(new VerificationResponse(
                token.getCode(),
                certificate.getOwnerName(),
                certificate.getOwnerEmail(),
                certificate.getIssuedAt().toString()
        ));
    }

    // DTO interno alla risposta
    record VerificationResponse(
            String code,
            String ownerName,
            String ownerEmail,
            String issueDate
    ) {}

}