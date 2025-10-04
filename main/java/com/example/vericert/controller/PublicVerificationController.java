package com.example.vericert.controller;

import com.example.vericert.domain.Certificate;
import com.example.vericert.domain.VerificationToken;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/v")
public class PublicVerificationController {

    private final VerificationTokenRepository verificationRepo;
    private final CertificateRepository certificateRepo;

    public PublicVerificationController(VerificationTokenRepository verificationRepo,
                                  CertificateRepository certificateRepo) {
        this.verificationRepo = verificationRepo;
        this.certificateRepo = certificateRepo;
    }

    /**
     * Verifica pubblica del certificato tramite codice QR
     * Esempio: GET /v/ABC123XYZ
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> verifyCertificate(@PathVariable("code") String code) {
        Optional<VerificationToken> tokenOpt = verificationRepo.findByCode(code);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        VerificationToken token = tokenOpt.get();
        Long certId = token.getCertificateId();
        Certificate certificate = certificateRepo.getById(certId);

        if (certificate.getStatus() == Certificate.Status.REVOKED) {
            return ResponseEntity.status(410) // Gone
                    .body("❌ Certificato revocato");
        }

        // Puoi restituire un DTO con i dati del certificato
        return ResponseEntity.ok(new VerificationResponse(
                token.getCode(),
                certificate.getOwnerName(),
                certificate.getOwnerEmail(),
                certificate.getCourseCode(),
                certificate.getIssuedAt().toString()
        ));
    }

    // DTO interno alla risposta
    record VerificationResponse(
            String code,
            String ownerName,
            String ownerEmail,
            String courseCode,
            String issueDate
    ) {}

    @GetMapping("/codes")
    public ResponseEntity<?> getValidCodes() {
        var codes = verificationRepo.findAll().stream()
                .map(VerificationToken::getCode)
                .toList();
        return ResponseEntity.ok(codes);
    }

}
