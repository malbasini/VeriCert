package com.example.vericert.repo;

import com.example.vericert.domain.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("select ts from TenantSettings ts where ts.subscriptionId is not null and ts.subscriptionId <> '' and ts.provider='PAYPAL'")
    List<TenantSettings> findTenantsWithPaypalSubscription();
}