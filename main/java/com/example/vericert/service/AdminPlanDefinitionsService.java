package com.example.vericert.service;

import com.example.vericert.domain.PlanDefinitions;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AdminPlanDefinitionsService {
    private final PlanDefinitionRepository repo;
    private final TenantSettingsRepository tenantSettingsRepo;
    public AdminPlanDefinitionsService(PlanDefinitionRepository repo,
                                       TenantSettingsRepository tenantSettingsRepo)
    {
        this.repo = repo;
        this.tenantSettingsRepo = tenantSettingsRepo;
    }

    public PlanDefinitions getPlan(String code)
    {
        return repo.findByCode(code).orElseThrow();
    }
    public List<PlanDefinitions> getPlans(){
        return repo.findAllPlains();
    }

    @Transactional
    public void activatePlan(Long tenantId, String planCode, String cycle, String checkoutSessionId, String pspRef) {
        PlanDefinitions plan = repo.findByCode(planCode).orElseThrow();
        Instant start = Instant.now();
        Instant end = "ANNUAL".equalsIgnoreCase(cycle) ? start.plus(365, ChronoUnit.DAYS) : start.plus(30, ChronoUnit.DAYS);
        TenantSettings ts = tenantSettingsRepo.findById(tenantId).orElseGet(() -> {TenantSettings
                            t = new TenantSettings(); t.setTenantId(tenantId);return t;
        });
        ts.setPlanCode(planCode);
        ts.setBillingCycle(cycle);
        ts.setCurrentPeriodStart(start);
        ts.setCurrentPeriodEnd(end);
        ts.setCertsPerMonth(plan.getCertsPerMonth());
        ts.setApiCallPerMonth(plan.getApiCallsPerMonth());
        ts.setStorageMb(plan.getStorageMb());
        ts.setSupport(plan.isSupportPriority());
        ts.setProvider(pspRef);
        ts.setCheckoutSessionId(checkoutSessionId);
        ts.setStatus("ACTIVE");
        ts.setUpdatedAt(Instant.now());
        tenantSettingsRepo.save(ts);
    }

}

