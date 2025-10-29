package com.example.vericert.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "usage_meter")
public class UsageMeter {

    @EmbeddedId
    private UsageMeterKey id;

    @Column(name = "certs_generated", nullable = false)
    private Integer certsGenerated = 0;

    @Column(name = "pdf_storage_mb", nullable = false, precision = 10, scale = 2)
    private BigDecimal pdfStorageMb = BigDecimal.ZERO;

    @Column(name = "api_calls", nullable = false)
    private Integer apiCalls = 0;

    @Column(name = "last_update_ts", nullable = false)
    private Instant lastUpdateTs = Instant.now();

    public UsageMeter() {}

    public UsageMeter(UsageMeterKey id) {
        this.id = id;
    }

    public UsageMeterKey getId() {
        return id;
    }

    public void setId(UsageMeterKey id) {
        this.id = id;
    }

    public Integer getCertsGenerated() {
        return certsGenerated;
    }

    public void setCertsGenerated(Integer certsGenerated) {
        this.certsGenerated = certsGenerated;
    }

    public BigDecimal getPdfStorageMb() {
        return pdfStorageMb;
    }

    public void setPdfStorageMb(BigDecimal pdfStorageMb) {
        this.pdfStorageMb = pdfStorageMb;
    }

    public Integer getApiCalls() {
        return apiCalls;
    }

    public void setApiCalls(Integer apiCalls) {
        this.apiCalls = apiCalls;
    }

    public Instant getLastUpdateTs() {
        return lastUpdateTs;
    }

    public void setLastUpdateTs(Instant lastUpdateTs) {
        this.lastUpdateTs = lastUpdateTs;
    }
}
