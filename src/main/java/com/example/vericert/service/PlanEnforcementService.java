package com.example.vericert.service;


import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.dto.UsageTotals;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class PlanEnforcementService {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final UsageMeterRepository usageMeterRepo;
    private final ZoneId zone = ZoneId.of("Europe/Rome");

    public PlanEnforcementService(TenantSettingsRepository tenantSettingsRepo,
                                  UsageMeterRepository usageMeterRepo) {
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.usageMeterRepo = usageMeterRepo;
    }

    // ========= CHECK OPERATIVI =========

    @Transactional(readOnly = true)
    public void checkCanIssueDocuments(Long tenantId) {
        TenantSettings s = load(tenantId);

        if (isExpired(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.PLAN_EXPIRED,
                    "Il piano è scaduto: rinnova per continuare a emettere documenti."
            );
        }

        if (isOverCertQuota(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.CERTIFICATE_QUOTA_EXCEEDED,
                    "Hai raggiunto il limite di documenti per il tuo piano."
            );
        }
    }

    public void checkCanStorePdf(Long tenantId, BigDecimal additionalMb) {
        TenantSettings ts = loadActiveSettings(tenantId);

        UsageTotals totals = currentMonthlyTotals(ts);

        BigDecimal limitMb = ts.getStorageMb();

        BigDecimal limit = limitMb;
        BigDecimal projected = totals.storageMb().add(additionalMb);

        if (projected.compareTo(limit) > 0) {
            throw new IllegalStateException("Limite storage mensile superato");
        }
    }
    // ----------------- helper -----------------

    private TenantSettings loadActiveSettings(Long tenantId) {
        TenantSettings ts = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        PlanStatus status = ts.getStatusEnum();
        if (status == null || status != PlanStatus.ACTIVE) {
            throw new IllegalStateException("Piano non attivo o scaduto");
        }

        return ts;
    }

    /**
     * Restituisce i totali SOLO della finestra mensile corrente
     * ancorata al current_period_start.
     */
    private UsageTotals currentMonthlyTotals(TenantSettings ts) {
        LocalDate periodStart = ts.getCurrentPeriodStart().atZone(zone).toLocalDate();
        LocalDate periodEnd = ts.getCurrentPeriodEnd().atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(zone);

        LocalDate[] window = UsageWindowCalculator.currentMonthlyWindow(periodStart, periodEnd, today);
        LocalDate from = window[0];
        LocalDate to = window[1];

        return usageMeterRepo.sumUsageForTenantBetweenDays(ts.getTenantId(), from, to);
    }
    @Transactional(readOnly = true)
    public void checkCanCallApi(Long tenantId) {
        TenantSettings s = load(tenantId);

        if (isExpired(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.PLAN_EXPIRED,
                    "Il piano è scaduto: rinnova per continuare a usare le API."
            );
        }

        if (isOverApiQuota(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.API_QUOTA_EXCEEDED,
                    "Hai raggiunto il limite di chiamate API per il tuo piano."
            );
        }
    }

    // ========= INFO PER LA DASHBOARD / BANNER =========

    @Transactional(readOnly = true)
    public CurrentPlanView buildCurrentPlanView(Long tenantId) {
        TenantSettings s = load(tenantId);

        LocalDate[] range = currentMonthlyRange(s);
        LocalDate from = range[0];
        LocalDate to = range[1];

        int usedCerts = usageMeterRepo.sumCertsInPeriod(tenantId, from, to);
        int usedApi   = usageMeterRepo.sumApiCallsInPeriod(tenantId, from, to);
        BigDecimal usedStorage = usageMeterRepo.sumStorageInPeriod(tenantId, from, to);

        long daysLeft = daysLeft(s);

        return new CurrentPlanView(
                s.getPlanCode(),
                s.getBillingCycle(),
                s.getStatus(),
                daysLeft,
                usedCerts,
                s.getCertsPerMonth(),
                usedApi,
                s.getApiCallPerMonth(),
                usedStorage,
                s.getStorageMb()
        );
    }


    @Transactional
    public void markExpiredIfNeeded(TenantSettings s) {
        if (isExpired(s) && !"EXPIRED".equalsIgnoreCase(s.getStatus())) {
            s.setStatus("EXPIRED");
            tenantSettingsRepo.save(s);
        }
    }

    // ========= PRIVATI =========

    private TenantSettings load(Long tenantId) {
        return tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings non trovate per tenant " + tenantId));
    }

    private boolean isExpired(TenantSettings s) {
        return s.getCurrentPeriodEnd() != null &&
                s.getCurrentPeriodEnd().isBefore(Instant.now());
    }

    private boolean isOverCertQuota(TenantSettings s) {
        if (s.getCertsPerMonth() <= 0) return false;

        LocalDate[] range = currentBillingRange(s);
        int used = usageMeterRepo.sumCertsInPeriod(
                s.getTenantId(),
                range[0],
                range[1]
        );
        return used >= s.getCertsPerMonth();
    }

    private boolean isOverApiQuota(TenantSettings s) {
        if (s.getApiCallPerMonth() <= 0) return false;

        LocalDate[] range = currentBillingRange(s);
        int used = usageMeterRepo.sumApiCallsInPeriod(
                s.getTenantId(),
                range[0],
                range[1]
        );
        return used >= s.getApiCallPerMonth();
    }

    private long daysLeft(TenantSettings s) {
        if (s.getCurrentPeriodEnd() == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(Instant.now(), s.getCurrentPeriodEnd());
    }

    private LocalDate[] currentBillingRange(TenantSettings s) {
        if (s.getCurrentPeriodStart() != null && s.getCurrentPeriodEnd() != null) {
            LocalDate from = s.getCurrentPeriodStart().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate to = s.getCurrentPeriodEnd().atZone(ZoneOffset.UTC).toLocalDate();
            return new LocalDate[]{from, to};
        }
        // fallback: mese corrente
        LocalDate today = LocalDate.now();
        return new LocalDate[]{today.withDayOfMonth(1), today};
    }

    private LocalDate[] currentMonthlyRange(TenantSettings s) {
        if (s.getCurrentPeriodStart() == null || s.getCurrentPeriodEnd() == null) {
            LocalDate today = LocalDate.now(zone);
            return new LocalDate[]{today.withDayOfMonth(1), today};
        }

        LocalDate periodStart = s.getCurrentPeriodStart().atZone(zone).toLocalDate();
        LocalDate periodEnd   = s.getCurrentPeriodEnd().atZone(zone).toLocalDate();
        LocalDate today       = LocalDate.now(zone);

        LocalDate[] window = UsageWindowCalculator.currentMonthlyWindow(periodStart, periodEnd, today);
        return new LocalDate[]{window[0], window[1]};
    }

}
