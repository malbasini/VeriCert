package com.example.vericert.service;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.UsageTotals;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class UsageAggregationService {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final UsageMeterRepository usageMeterRepo;

    public UsageAggregationService(TenantSettingsRepository tenantSettingsRepo,
                                   UsageMeterRepository usageMeterRepo) {

        this.tenantSettingsRepo = tenantSettingsRepo;
        this.usageMeterRepo = usageMeterRepo;
    }

    public UsageTotals getCurrentPeriodTotals(Long tenantId) {
        TenantSettings ts = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        ZoneId zone = ZoneId.of("Europe/Rome"); // oppure ZoneId.of("Europe/Rome")

        LocalDate from = ts.getCurrentPeriodStart().atZone(zone).toLocalDate();
        LocalDate to = ts.getCurrentPeriodEnd().atZone(zone).toLocalDate();

        // Se vuoi evitare che "oggi" dopo il reset sballi, puoi clampare su oggi:
        LocalDate today = LocalDate.now(zone);
        if (to.isAfter(today)) {
            to = today;
        }

        return usageMeterRepo.sumUsageForTenantBetweenDays(tenantId, from, to);
    }
}
