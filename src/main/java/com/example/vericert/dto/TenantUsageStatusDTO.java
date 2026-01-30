package com.example.vericert.dto;

import java.math.BigDecimal;

public class TenantUsageStatusDTO {

    private final Long tenantId;

    // consumo attuale
    private final BigDecimal storageMbUsed;
    private final Integer certsGeneratedToday;
    private final Integer apiCallsToday;

    // limiti contrattuali
    private final BigDecimal maxStorageMb;
    private final Integer maxCertsPerMonth;
    private final Integer maxApiCallsPerDay;

    // semaforo globale sullo storage (ok / warn / critical)
    private final String storageStatus; // "OK", "WARN", "CRITICAL"

    public TenantUsageStatusDTO(Long tenantId,
                                BigDecimal storageMbUsed,
                                Integer certsGeneratedToday,
                                Integer apiCallsToday,
                                BigDecimal maxStorageMb,
                                Integer maxCertsPerMonth,
                                Integer maxApiCallsPerDay,
                                String storageStatus) {

        this.tenantId = tenantId;
        this.storageMbUsed = storageMbUsed;
        this.certsGeneratedToday = certsGeneratedToday;
        this.apiCallsToday = apiCallsToday;
        this.maxStorageMb = maxStorageMb;
        this.maxCertsPerMonth = maxCertsPerMonth;
        this.maxApiCallsPerDay = maxApiCallsPerDay;
        this.storageStatus = storageStatus;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public BigDecimal getStorageMbUsed() {
        return storageMbUsed;
    }

    public Integer getCertsGeneratedToday() {
        return certsGeneratedToday;
    }

    public Integer getApiCallsToday() {
        return apiCallsToday;
    }

    public BigDecimal getMaxStorageMb() {
        return maxStorageMb;
    }

    public Integer getMaxCertsPerMonth() {
        return maxCertsPerMonth;
    }

    public Integer getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public String getStorageStatus() {
        return storageStatus;
    }
}
