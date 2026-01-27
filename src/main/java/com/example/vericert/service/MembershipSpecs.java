package com.example.vericert.service;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSpecs {
    public static Specification<Membership> byTenant(Long tenantId) {
        return (root, cq, cb) -> cb.equal(root.get("tenant").get("id"), tenantId);
    }
    public static Specification<Membership> keyword(String q) {
        if (q == null || q.isBlank()) return (root, cq, cb) -> cb.conjunction();
        String kw = "%" + q.trim().toLowerCase() + "%";
        return (root, cq, cb) -> {
            Join<Membership, User> u = root.join("user");
            return cb.or(
                    cb.like(cb.lower(u.get("userName")), kw),
                    cb.like(cb.lower(u.get("email")), kw)
            );
        };
    }
}
