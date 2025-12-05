package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.TenantSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FreePlanRenewalScheduler {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanUsageResetService planUsageResetService;

    public FreePlanRenewalScheduler(TenantSettingsRepository tenantSettingsRepo,
                                    PlanUsageResetService planUsageResetService) {
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.planUsageResetService = planUsageResetService;
    }

    /**
     * Esegue ogni notte (03:15) e rinnova i piani FREE mensili
     * il cui current_period_end è passato, azzerando i contatori
     * della usage_meter.
     */
    @Scheduled(cron = "0 43 3 * * *")
    @Transactional
    public void renewFreeMonthlyPlans() {
        Instant now = Instant.now();

        List<TenantSettings> expiredFree = tenantSettingsRepo
                .findByPlanCodeAndBillingCycleAndStatusAndCurrentPeriodEndBefore(
                        "FREE",
                        "MONTHLY",
                        PlanStatus.ACTIVE.name(),   // solo piani FREE attivi
                        now
                );

        for (TenantSettings ts : expiredFree) {
            Long tenantId = ts.getTenantId();

            // nuovo periodo: parte dalla fine del precedente e dura 1 mese
            Instant newStart = ts.getCurrentPeriodEnd();
            Instant newEnd = newStart.atZone(ZoneOffset.UTC)
                    .plus(1, ChronoUnit.MONTHS)
                    .toInstant();

            ts.setCurrentPeriodStart(newStart);
            ts.setCurrentPeriodEnd(newEnd);
            ts.setStorageMb(0L);
            tenantSettingsRepo.save(ts);

            // azzera usage_meter e riallinea i limiti (FREE)
            planUsageResetService.resetUsageForNewPeriod(tenantId);

            System.out.println(">>> FREE plan rinnovato e usage reset per tenant " + tenantId);
        }
    }
}
