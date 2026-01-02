package com.example.vericert.repo;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TenantSettingsRepository extends JpaRepository<TenantSettings, Long> {

    Optional<TenantSettings> findByTenantId(Long tenantId);
    List<TenantSettings> findByPlanCodeAndBillingCycleAndStatusAndCurrentPeriodEndBefore(
            String planCode,
            String billingCycle,
            String status,
            Instant before
    );
}