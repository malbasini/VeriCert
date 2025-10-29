package com.example.vericert.service;

import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.domain.Tenant;
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
}