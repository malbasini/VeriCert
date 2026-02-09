package com.example.vericert.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyUsageDTO {
    private final Long tenantId;
    private final LocalDate day;
    private final Integer certsGenerated;
    private final Integer apiCalls;
    private final BigDecimal pdfStorageMb;
    private final Integer verificationsCount;   // NEW

    public DailyUsageDTO(Long tenantId,
                         LocalDate day,
                         Integer certsGenerated,
                         Integer apiCalls,
                         BigDecimal pdfStorageMb,
                         Integer verificationsCount) {
        this.tenantId = tenantId;
        this.day = day;
        this.certsGenerated = certsGenerated;
        this.apiCalls = apiCalls;
        this.pdfStorageMb = pdfStorageMb;
        this.verificationsCount = verificationsCount;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public LocalDate getDay() {
        return day;
    }

    public Integer getCertsGenerated() {
        return certsGenerated;
    }

    public Integer getApiCalls() {
        return apiCalls;
    }

    public BigDecimal getPdfStorageMb() {
        return pdfStorageMb;
    }

    public Integer getVerificationsCount() {
        return verificationsCount;
    }
}
