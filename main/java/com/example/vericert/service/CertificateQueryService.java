package com.example.vericert.service;

import com.example.vericert.domain.Certificate;
import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.repo.CertificateRepository;
import com.example.vericert.repo.CertificateSpecs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CertificateQueryService {

    private final CertificateRepository repo;

    public CertificateQueryService(CertificateRepository repo) {
        this.repo = repo;
    }

    public Page<Certificate> search(
            Long tenantId,
            String q,
            Stato status,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        var spec = CertificateSpecs.byFilters(tenantId, q, status, from, to);
        return repo.findAll(spec, pageable);
    }
}