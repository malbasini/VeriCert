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

    public VerificationOutcome verify(Long tenantId, String compactJws, Source src, String code) throws IOException {
        // recupera dal token DB il certId per caricare i bytes:
        Long certId = repository.findByCode(code).get().getCertificateId();
        Certificate c = repo.findById(certId).orElseThrow();

        // in pratica: prima parse o chiama verifier una prima volta solo per leggere certId dal payload
        // qui semplifico assumendo che tu abbia i bytes:
        byte[] certBytes = certStorage.loadPdfBytes(tenantId,c.getSerial());

        VerificationOutcome out = verifier.verify(compactJws, certBytes);
        // metering
        usageMeterService.incrementVerifications(tenantId);
        if (src == Source.API) usageMeterService.incrementApiCalls(tenantId);

        return out;
    }

    private String extractJti(String compactJws) {
        try {
            var j = JWSObject.parse(compactJws);
            return (String) j.getPayload().toJSONObject().get("jti");
        } catch (Exception e) { return null; }
    }
}
