package com.example.vericert.repo;

// Specs (builder dei filtri)

import com.example.vericert.domain.Certificate;
import com.example.vericert.enumerazioni.Stato;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CertificateSpecs {

    public static Specification<Certificate> byFilters(
            Long tenantId,
            String q,                      // ricerca libera su serial, ownerName, courseCode
            Stato status,      // ISSUED / REVOKED (o null)
            Instant issuedFrom,            // data/emissione >= (o null)
            Instant issuedTo               // data/emissione <  (o null)
    ) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // multitenancy (se NON usi già il filtro Hibernate @Filter)
            if (tenantId != null) {
                ps.add(cb.equal(root.get("tenantId"), tenantId));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("serial")), like),
                        cb.like(cb.lower(root.get("ownerName")), like)
                ));
            }

            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }

            if (issuedFrom != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("issuedAt"), issuedFrom));
            }

            if (issuedTo != null) {
                ps.add(cb.lessThan(root.get("issuedAt"), issuedTo));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
