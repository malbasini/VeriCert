package com.example.vericert.service;


import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class UsageLimitsViewService {

    private final TenantRepository tenantRepo;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final UsageAggregationService usageAggregationService;
    private final PlanEnforcementService planEnforcementService;

    public UsageLimitsViewService(TenantRepository tenantRepo,
                                  TenantSettingsRepository tenantSettingsRepo,
                                  UsageAggregationService usageAggregationService,
                                  PlanEnforcementService planEnforcementService) {
        this.tenantRepo = tenantRepo;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.usageAggregationService = usageAggregationService;
        this.planEnforcementService = planEnforcementService;
    }

    public CurrentPlanView buildUsageAndLimits(Long tenantId) {
        Tenant tenant = tenantRepo.getTenantById(tenantId);
        TenantSettings ts = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        CurrentPlanView totals = planEnforcementService.buildCurrentPlanView(tenantId);

        return new CurrentPlanView(
                tenant.getPlan().name(),
                ts.getBillingCycle(),
                totals.getStatus(),
                totals.getDaysLeft(),
                totals.getUsedCerts(),
                totals.getCertLimit(),
                totals.getUsedApi(),
                totals.getApiLimit(),
                totals.getUsedStorage(),
                totals.getStorageLimit()


        );
    }
}
