package com.example.vericert.service;

import com.example.vericert.domain.Certificate;
import com.example.vericert.dto.VerificationOutcome;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.VerificationTokenRepository;
import com.nimbusds.jose.JWSObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class QrVerificationService {

    private final QrVerifier verifier;
    private final UsageMeterService usageMeterService;
    private final CertificateStorageService certStorage;// tuo: carica i bytes del PDF/asset
    private final VerificationTokenRepository repository;
    private final CertificateRepository repo;


    public enum Source { API, WEB }

    public QrVerificationService(QrVerifier verifier,
                                 UsageMeterService usageMeterService,
                                 CertificateStorageService certStorage,
                                 VerificationTokenRepository repository,
                                 CertificateRepository repo) {
        this.verifier = verifier;
        this.usageMeterService = usageMeterService;
        this.certStorage = certStorage;
        this.repository = repository;
        this.repo = repo;
    }

    public VerificationOutcome verify(Long tenantId, String compactJws, String code) throws IOException {
        // recupera dal token DB il certId per caricare i bytes:
        Long certId = repository.findByCode(code).get().getCertificateId();
        Certificate c = repo.findById(certId).orElseThrow();

        // in pratica: prima parse o chiama verifier una prima volta solo per leggere certId dal payload
        // qui semplifico assumendo che tu abbia i bytes:
        byte[] certBytes = certStorage.loadPdfBytes(tenantId,c.getSerial());

        VerificationOutcome out = verifier.verify(compactJws, certBytes);
        // metering
        usageMeterService.incrementVerifications(tenantId);
        usageMeterService.incrementApiCalls(tenantId);

        return out;
    }

    private String extractJti(String compactJws) {
        try {
            var j = JWSObject.parse(compactJws);
            return (String) j.getPayload().toJSONObject().get("jti");
        } catch (Exception e) { return null; }
    }

    public VerificationOutcome verifyUploadedPdf(
            Long tenantId,
            String compactJws,
            Source src,
            String code,
            byte[] uploadedPdfBytes
    ) throws IOException {

        // (opzionale ma consigliato) valida che il code esista e che sia associato a un certificato
        // serve anche per metering e per evitare che qualcuno chiami verify con token random.
        Long certId = repository.findByCode(code).orElseThrow().getCertificateId();
        repo.findById(certId).orElseThrow(); // assicura che il certificato esista

        VerificationOutcome out = verifier.verify(compactJws, uploadedPdfBytes);

        usageMeterService.incrementVerifications(tenantId);
        if (src == Source.API) usageMeterService.incrementApiCalls(tenantId);

        return out;
    }




}
