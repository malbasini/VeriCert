package com.example.vericert.repo;

import com.example.vericert.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findIdByName(String name);

    Optional<Tenant> findByName(String tenantName);
}
