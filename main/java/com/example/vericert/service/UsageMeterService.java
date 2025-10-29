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

    public UsageMeterService(UsageMeterRepository usageMeterRepository) {
        this.usageMeterRepository = usageMeterRepository;
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
    }

    @Transactional
    public void incrementApiCalls(Long tenantId) {
        UsageMeter m = getOrCreateToday(tenantId);
        m.setApiCalls(m.getApiCalls() + 1);
        m.setLastUpdateTs(Instant.now());
        usageMeterRepository.save(m);
    }

    @Transactional
    public void updateStorageMb(Long tenantId, BigDecimal currentStorageMb) {
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
