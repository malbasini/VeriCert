package com.example.vericert.service;

import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterKey;
import com.example.vericert.repo.UsageMeterRepository;
import com.example.vericert.dto.DailyUsageDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class UsageMeterService {

    private final UsageMeterRepository usageMeterRepository;
    private final StorageUsageService storageUsageService;

    public UsageMeterService(UsageMeterRepository usageMeterRepository,
                             StorageUsageService storageUsageService) {
        this.usageMeterRepository = usageMeterRepository;
        this.storageUsageService = storageUsageService;
    }

    private UsageMeter getOrCreateToday(Long tenantId) {
        LocalDate today = LocalDate.now();
        return usageMeterRepository
                .findByTenantAndDay(tenantId, today)
                .orElseGet(() -> {
                    UsageMeter m = new UsageMeter(new UsageMeterKey(tenantId, today));
                    m.setCertsGenerated(0);
                    m.setApiCalls(0);
                    m.setPdfStorageMb(BigDecimal.ZERO);
                    m.setLastUpdateTs(Instant.now());
                    return usageMeterRepository.save(m);
                });
    }

    @Transactional
    public void incrementCertsGenerated(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setCertsGenerated(m.getCertsGenerated() + 1);
        m.setLastUpdateTs(Instant.now());
        usageMeterRepository.save(m);
        // aggiorniamo anche lo storage, perché spesso generare un certificato = salvare un PDF
        refreshStorageForTenant(tenantId);
    }

    @Transactional
    public void incrementApiCalls(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setApiCalls(m.getApiCalls() + 1);
        m.setLastUpdateTs(Instant.now());
        usageMeterRepository.save(m);
    }
    @Transactional
    public void incrementVerifications(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setVerificationsCount(m.getVerificationsCount() + 1);
        m.setLastUpdateTs(Instant.now());
        usageMeterRepository.save(m);
    }
    /**
     * Forza un ricalcolo dello spazio occupato dal tenant e lo salva nella riga di oggi.
     * Puoi chiamarlo:
     * - dopo aver generato/eliminato un PDF
     * - in uno scheduler periodico
     */
    @Transactional
    public void refreshStorageForTenant(Long tenantId) {
        BigDecimal currentStorageMb = storageUsageService.calculateCurrentStorageMb(tenantId);

        UsageMeter m = getOrCreateToday(tenantId);
        m.setPdfStorageMb(currentStorageMb);
        m.setLastUpdateTs(Instant.now());
        usageMeterRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<DailyUsageDTO> getUsageHistoryForTenant(Long tenantId, int daysBack) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(daysBack);
        return usageMeterRepository.getUsageHistoryForTenant(tenantId, from, to);
    }

    @Transactional(readOnly = true)
    public List<DailyUsageDTO> getTopTenantsToday() {
        LocalDate today = LocalDate.now();
        return usageMeterRepository.getTopTenantsToday(today);
    }
}
