package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.TenantSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FreePlanRenewalScheduler {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanUsageResetService planUsageResetService;
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

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
    @Scheduled(cron = "0 30 3 * * *")
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
            ZonedDateTime zdt = newStart.atZone(ZONE);
            Instant newEnd = newStart.atZone(ZONE)
                    .plus(1, ChronoUnit.MONTHS)
                    .toInstant();

            ts.setCurrentPeriodStart(zdt.toInstant());
            ts.setCurrentPeriodEnd(newEnd);
            ts.setStorageMb(BigDecimal.valueOf(0));
            tenantSettingsRepo.save(ts);

            // azzera usage_meter e riallinea i limiti (FREE)
            planUsageResetService.resetUsageForNewPeriod(tenantId);

            System.out.println(">>> FREE plan rinnovato e usage reset per tenant " + tenantId);
        }
    }
}
