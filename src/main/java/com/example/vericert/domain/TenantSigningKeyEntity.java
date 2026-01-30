package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_signing_key")
public class TenantSigningKeyEntity {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "kid", length = 255, nullable = false)
    private String kid;

    @Column(name = "status", length = 32, nullable = false)
    private String status; // ACTIVE/RETIRED

    @Column(name = "assigned_ts")
    private LocalDateTime assignedTs;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAssignedTs() {
        return assignedTs;
    }

    public void setAssignedTs(LocalDateTime assignedTs) {
        this.assignedTs = assignedTs;
    }
}
