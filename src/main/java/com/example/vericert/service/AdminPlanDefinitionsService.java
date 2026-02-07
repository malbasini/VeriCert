package com.example.vericert.service;

import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class AdminPlanDefinitionsService {
    private final PlanDefinitionRepository repo;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final TenantRepository tenantRepo;
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    public AdminPlanDefinitionsService(PlanDefinitionRepository repo,
                                       TenantSettingsRepository tenantSettingsRepo,
                                       TenantRepository tenantRepo)
    {
        this.repo = repo;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.tenantRepo = tenantRepo;
    }

    public PlanDefinition getPlan(String code)
    {
        return repo.findByCode(code).orElseThrow();
    }
    public List<PlanDefinition> getPlans(){
        return repo.findAllPlains();
    }

    @Transactional
    public void activatePlan(Long tenantId, String planCode, String cycle, String checkoutSessionId, String pspRef) {
        PlanDefinition plan = repo.findByCode(planCode).orElseThrow();
        Instant start = Instant.now();
        ZonedDateTime zdt = start.atZone(ZONE);
        TenantSettings ts = tenantSettingsRepo.findById(tenantId).orElseGet(() -> {TenantSettings
                            t = new TenantSettings(); t.setTenantId(tenantId);return t;
        });
        ZonedDateTime end = switch (cycle) {
            case "ANNUAL"  -> zdt.plusYears(1);
            default        -> zdt.plusMonths(1);
        };
        ts.setPlanCode(planCode);
        ts.setBillingCycle(cycle);
        ts.setCurrentPeriodStart(zdt.toInstant());
        ts.setCurrentPeriodEnd(end.toInstant());
        ts.setCertsPerMonth(plan.getCertsPerMonth());
        ts.setApiCallPerMonth(plan.getApiCallsPerMonth());
        ts.setStorageMb(BigDecimal.valueOf(plan.getStorageMb()));
        ts.setSupport(plan.isSupportPriority());
        ts.setProvider(pspRef);
        ts.setCheckoutSessionId(checkoutSessionId);
        ts.setStatus("ACTIVE");
        ts.setUpdatedAt(Instant.now());
        tenantSettingsRepo.save(ts);
        Tenant t = tenantRepo.findById(tenantId).orElseThrow();
        t.setPlan(Plan.valueOf(planCode));
        tenantRepo.save(t);
    }
}

