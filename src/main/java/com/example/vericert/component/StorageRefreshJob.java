package com.example.vericert.component;

import com.example.vericert.domain.Tenant;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.service.UsageMeterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job periodico che riallinea lo spazio occupato su disco.
 * Frequenza di esempio: ogni 15 minuti.
 */
@Component
public class StorageRefreshJob {

    private final UsageMeterService usageMeterService;
    private final TenantRepository tenantRepository; // <-- se ce l'hai

    public StorageRefreshJob(UsageMeterService usageMeterService,
                             TenantRepository tenantRepository ) {
        this.usageMeterService = usageMeterService;
        this.tenantRepository = tenantRepository;
    }

    @Scheduled(cron = "0 */15 * * * *") // ogni 15 minuti al minuto 0,15,30,45
    public void refreshAllTenantsStorage() {

        // Qui devi recuperare tutti i tenantId attivi.
        List<Tenant> allTenantIds = tenantRepository.findAll().stream().filter(z -> z.getStatus().equals("ACTIVE")).toList();

        for (Tenant tenant : allTenantIds) {
            usageMeterService.refreshStorageForTenant(tenant.getId());
        }
    }
}
