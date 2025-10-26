package com.example.vericert.repo;

import com.example.vericert.domain.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {

    Optional<TenantSettings> findByTenantId(Long tenantId);
}