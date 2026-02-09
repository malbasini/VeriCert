package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.PlanLimits;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PlanUsageResetService {

    private final UsageMeterRepository usageMeterRepo;
    private final TenantSettingsRepository tenantSettingsRepo;

    public PlanUsageResetService(UsageMeterRepository usageMeterRepo,
                                 TenantSettingsRepository tenantSettingsRepo) {
        this.usageMeterRepo = usageMeterRepo;
        this.tenantSettingsRepo = tenantSettingsRepo;
    }

    /**
     * Resetta i contatori di usage (certificati, API, storage) per il tenant
     * e riallinea i limiti mensili in TenantSettings in base al piano.
     *
     * Da chiamare quando parte un NUOVO PERIODO di fatturazione
     * (es. dopo un rinnovo Stripe/PayPal, cambio piano, ecc.).
     */
    @Transactional
    public void resetUsageForNewPeriod(Long tenantId) {

        TenantSettings settings = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings non trovate per tenant " + tenantId));

        String planCode = settings.getPlanCode();
        PlanLimits limits = PlanLimits.fromPlanCode(planCode);

        // 1) Azzero i contatori nella usage_meter
        int rows = usageMeterRepo.resetUsageForTenant(tenantId, Instant.now());
        System.out.println(">>> Usage reset per tenant " + tenantId + " su " + rows + " righe usage_meter.");

        // 2) Riallineo i limiti nel TenantSettings (per sicurezza/coerenza)
        settings.setCertsPerMonth(limits.getCertsPerMonth());
        settings.setApiCallPerMonth(limits.getApiCallsPerMonth());
        settings.setStorageMb(BigDecimal.valueOf(limits.getStorageMbPerMonth()));

        tenantSettingsRepo.save(settings);
    }
}