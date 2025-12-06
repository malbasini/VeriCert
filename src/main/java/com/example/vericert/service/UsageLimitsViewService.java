package com.example.vericert.service;


import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.UsageAndLimitsView;
import com.example.vericert.dto.UsageTotals;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageLimitsViewService {

    private final TenantRepository tenantRepo;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final UsageAggregationService usageAggregationService;

    public UsageLimitsViewService(TenantRepository tenantRepo,
                                  TenantSettingsRepository tenantSettingsRepo,
                                  UsageAggregationService usageAggregationService) {
        this.tenantRepo = tenantRepo;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.usageAggregationService = usageAggregationService;
    }

    public UsageAndLimitsView buildUsageAndLimits(Long tenantId) {
        Tenant tenant = tenantRepo.getTenantById(tenantId);
        TenantSettings ts = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        UsageTotals totals = usageAggregationService.getCurrentPeriodTotals(tenantId);

        return new UsageAndLimitsView(
                tenant.getPlan().name(),
                ts.getBillingCycle(),

                totals.certs(),
                totals.apiCalls(),
                totals.storageMb(),

                ts.getCertsPerMonth(),
                ts.getApiCallPerMonth(),
                ts.getStorageMb()
        );
    }
}
