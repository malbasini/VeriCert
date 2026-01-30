package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class UsageMeterKey implements Serializable {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "usage_day")
    private LocalDate usageDay;

    public UsageMeterKey() {}

    public UsageMeterKey(Long tenantId, LocalDate usageDay) {
        this.tenantId = tenantId;
        this.usageDay = usageDay;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public LocalDate getUsageDay() {
        return usageDay;
    }

    public void setUsageDay(LocalDate usageDay) {
        this.usageDay = usageDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsageMeterKey that = (UsageMeterKey) o;
        return Objects.equals(tenantId, that.tenantId)
                && Objects.equals(usageDay, that.usageDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, usageDay);
    }
}
