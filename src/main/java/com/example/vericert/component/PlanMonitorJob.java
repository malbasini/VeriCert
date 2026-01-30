package com.example.vericert.component;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.PlanEnforcementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanMonitorJob {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanEnforcementService enforcementService;

    public PlanMonitorJob(TenantSettingsRepository tenantSettingsRepo,
                          PlanEnforcementService enforcementService) {
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.enforcementService = enforcementService;
    }

    // ogni notte alle 03:00
    @Scheduled(cron = "0 0 3 * * *")
    public void checkAllTenants() {
        for (TenantSettings s : tenantSettingsRepo.findAll()) {
            enforcementService.markExpiredIfNeeded(s);

            long daysLeft = enforcementService.buildCurrentPlanView(s.getTenantId()).getDaysLeft();
            if (daysLeft >= 0 && daysLeft <= 5 && !"EXPIRED".equalsIgnoreCase(s.getStatus())) {
                // TODO: integra un MailService o invia notifica
                System.out.printf("⚠️ Tenant %d: piano in scadenza tra %d giorni%n",
                        s.getTenantId(), daysLeft);
            }
        }
    }
}
