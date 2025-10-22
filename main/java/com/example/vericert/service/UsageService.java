package com.example.vericert.service;

import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterId;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.UsageMeterRepository;
import org.springframework.stereotype.Service;
import java.time.YearMonth;
//Consumi nel mese
@Service
public class UsageService {

    private final UsageMeterRepository repo; // CRUD su usage_meter

    private final TenantRepository repoTenant;

    public UsageService(UsageMeterRepository repo, TenantRepository repoTenant) {
        this.repo = repo;
        this.repoTenant = repoTenant;
    }

    public void assertCanIssue(Long tenantId, Plan plan) {
        String ym = YearMonth.now().toString();//Anno + Mese
        UsageMeter m = repo.findByIdTenantIdAndIdYm(tenantId, ym).orElseGet(() -> new UsageMeter(tenantId, ym));
        UsageMeterId umid = new UsageMeterId(tenantId, ym);
        m.setId(umid);
        int limit = switch (plan) {
            case FREE -> 20; case PRO -> 500; case BUSINESS -> 5000; case ENTERPRISE -> Integer.MAX_VALUE; };
        if (m.getCertCount() >= limit) throw new IllegalStateException("Limite attestati mensile raggiunto per il piano");
        Tenant t = repoTenant.getById(tenantId);
        m.setTenant(t);
        repo.save(m);
    }
}