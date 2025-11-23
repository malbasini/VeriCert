package com.example.vericert.service;


import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.exception.PlanLimitExceededException;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import com.example.vericert.dto.CurrentPlanView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class PlanEnforcementService {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final UsageMeterRepository usageMeterRepo;

    public PlanEnforcementService(TenantSettingsRepository tenantSettingsRepo,
                                  UsageMeterRepository usageMeterRepo) {
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.usageMeterRepo = usageMeterRepo;
    }

    // ========= CHECK OPERATIVI =========

    @Transactional(readOnly = true)
    public void checkCanIssueCertificate(Long tenantId) {
        TenantSettings s = load(tenantId);

        if (isExpired(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.PLAN_EXPIRED,
                    "Il piano è scaduto: rinnova per continuare a emettere certificati."
            );
        }

        if (isOverCertQuota(s)) {
            throw new PlanLimitExceededException(
                    tenantId,
                    PlanViolationType.CERTIFICATE_QUOTA_EXCEEDED,
                    "Hai raggiunto il limite di certificati per il tuo piano."
            );
        }
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

        LocalDate[] range = currentBillingRange(s);
        LocalDate from = range[0];
        LocalDate to = range[1];

        int usedCerts = usageMeterRepo.sumCertsInPeriod(tenantId, from, to);
        int usedApi = usageMeterRepo.sumApiCallsInPeriod(tenantId, from, to);

        long daysLeft = daysLeft(s);

        return new CurrentPlanView(
                s.getPlanCode(),
                s.getBillingCycle(),
                s.getStatus(),
                daysLeft,
                usedCerts,
                s.getCertsPerMonth(),
                usedApi,
                s.getApiCallPerMonth()
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
}

