package com.example.vericert.repo;

import com.example.vericert.domain.TenantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantProfileRepository extends JpaRepository<TenantProfile, Long> {
    Optional<TenantProfile> findByTenantId(Long tenantId);
}
