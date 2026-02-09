package com.example.vericert.service;


import com.example.vericert.domain.TenantSettings;
import com.example.vericert.dto.DailyUsageDTO;
import com.example.vericert.dto.TenantUsageStatusDTO;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.repo.UsageMeterRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenantUsageStatusService {

    private final UsageMeterRepository usageMeterRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    public TenantUsageStatusService(UsageMeterRepository usageMeterRepository,
                                    TenantSettingsRepository tenantSettingsRepository) {
        this.usageMeterRepository = usageMeterRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
    }

    /**
     * Ritorna lo "stato consumo" per tutti i tenant che oggi hanno attività,
     * arricchito con i limiti e il semaforo.
     *
     * Questo output è perfetto da buttare diretto nella dashboard admin.
     */
    public List<TenantUsageStatusDTO> buildTodayStatusForAllTenants() {

        LocalDate today = LocalDate.now();

        // Prendiamo l'elenco usage dei tenant di oggi, ordinati per certsGenerated DESC
        List<DailyUsageDTO> rawToday = usageMeterRepository.getTopTenantsToday(today);

        List<TenantUsageStatusDTO> result = new ArrayList<>();

        for (DailyUsageDTO usage : rawToday) {
            Long tenantId = usage.getTenantId();

            TenantSettings settings = tenantSettingsRepository.findByTenantId(tenantId).orElseThrow();

            BigDecimal used = usage.getPdfStorageMb() != null
                    ? usage.getPdfStorageMb()
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal maxStorage = settings.getStorageMb();

            String status = computeStorageStatus(used, maxStorage);

            TenantUsageStatusDTO dto = new TenantUsageStatusDTO(
                    tenantId,
                    used,
                    usage.getCertsGenerated(),
                    usage.getApiCalls(),
                    maxStorage,
                    Math.toIntExact(settings.getCertsPerMonth()),
                    Math.toIntExact(settings.getApiCallPerMonth()),
                    status
            );

            result.add(dto);
        }

        return result;
    }

    private String computeStorageStatus(BigDecimal used, BigDecimal max) {
        if (max == null || max.compareTo(BigDecimal.ZERO) <= 0) {
            // piano "illimitato" o limite non impostato -> sempre OK
            return "OK";
        }

        if (used == null) {
            return "OK";
        }

        // ratio = used / max
        BigDecimal ratio = used.divide(max, 4, RoundingMode.HALF_UP);

        // >100%
        if (ratio.compareTo(new BigDecimal("1.00")) > 0) {
            return "CRITICAL";
        }

        // >=80%
        if (ratio.compareTo(new BigDecimal("0.80")) >= 0) {
            return "WARN";
        }

        return "OK";
    }
}
