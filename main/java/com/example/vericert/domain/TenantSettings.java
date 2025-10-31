package com.example.vericert.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    // JSON salvato come testo normale -> NIENTE @Lob
    @Column(
            name = "json_settings",
            nullable = false,
            columnDefinition = "JSON"
    )
    private String jsonSettings;

    @Column(name = "max_storage_mb", precision = 10, scale = 2)
    private BigDecimal maxStorageMb; // es 500.00

    @Column(name = "max_certs_per_month")
    private Integer maxCertsPerMonth; // es 1000

    @Column(name = "max_api_calls_per_day")
    private Integer maxApiCallsPerDay; // es 2000

    // Lasciamo che MySQL lo mantenga con DEFAULT ... ON UPDATE ...
    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Instant updatedAt;

    public TenantSettings() {}

    public TenantSettings(Long tenantId, String jsonSettings) {
        this.tenantId = tenantId;
        this.jsonSettings = jsonSettings;
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getJsonSettings() { return jsonSettings; }
    public void setJsonSettings(String jsonSettings) { this.jsonSettings = jsonSettings; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; } // opzionale, ma la puoi lasciare

    public BigDecimal getMaxStorageMb() {
        return maxStorageMb;
    }

    public void setMaxStorageMb(BigDecimal maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    public Integer getMaxCertsPerMonth() {
        return maxCertsPerMonth;
    }

    public void setMaxCertsPerMonth(Integer maxCertsPerMonth) {
        this.maxCertsPerMonth = maxCertsPerMonth;
    }

    public Integer getMaxApiCallsPerDay() {
        return maxApiCallsPerDay;
    }

    public void setMaxApiCallsPerDay(Integer maxApiCallsPerDay) {
        this.maxApiCallsPerDay = maxApiCallsPerDay;
    }
}
