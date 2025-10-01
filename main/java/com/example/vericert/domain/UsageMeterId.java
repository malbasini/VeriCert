package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UsageMeterId implements Serializable {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "ym", nullable = false, length = 7)
    private String ym;

    public UsageMeterId() {}

    public UsageMeterId(Long tenantId, String ym) {
        this.tenantId = tenantId;
        this.ym = ym;
    }

    // getters/setters
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getYm() { return ym; }
    public void setYm(String ym) { this.ym = ym; }

    // equals/hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsageMeterId that)) return false;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(ym, that.ym);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, ym);
    }
}