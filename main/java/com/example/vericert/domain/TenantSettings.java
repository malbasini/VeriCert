package com.example.vericert.domain;

import jakarta.persistence.*;
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
}
