package com.example.vericert.repo;

import com.example.vericert.domain.Invoice;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecs {

    public static Specification<Invoice> byFilters(
            Long tenantId,
            String q
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
                        cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("customerEmail")), like),
                        cb.like(cb.lower(root.get("publicCode")), like)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
