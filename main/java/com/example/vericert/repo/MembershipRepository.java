package com.example.vericert.repo;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.MembershipId;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, MembershipId> {
    Optional<Membership> findByUser(User user);
    boolean existsByTenant(Tenant tenant);
    Membership findByTenant_Id(Long tenantId);
}
