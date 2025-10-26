package com.example.vericert.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    // Salviamo il JSON come stringa grezza.
    // Se vuoi, più avanti, puoi mappare oggetti embedded + @Convert.
    @Lob
    @Column(name = "json_settings", nullable = false, columnDefinition = "JSON")
    private String jsonSettings;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    // costruttori
    public TenantSettings() {}

    public TenantSettings(Long tenantId, String jsonSettings) {
        this.tenantId = tenantId;
        this.jsonSettings = jsonSettings;
    }

    // getter/setter
    public Long getTenantId() { return tenantId; }

    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getJsonSettings() { return jsonSettings; }

    public void setJsonSettings(String jsonSettings) { this.jsonSettings = jsonSettings; }

    public Instant getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
