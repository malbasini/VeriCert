package com.example.vericert.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;


@Entity
@Table(name="usage_meter")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class UsageMeter {

    @EmbeddedId
    private UsageMeterId id;
    @Column(nullable=false,insertable=false,updatable=false)
    private String ym;
    @Column(name="cert_count",nullable=false)
    private int certCount = 0;
    @Column(name="api_calls", nullable=false)
    private int apiCalls = 0;
    @Column(name="storage_bytes",nullable=false)
    private Long storageBytes = 0L;

    // relazione Tenant → senza duplicare tenant_id
    @MapsId("tenantId") // dice a Hibernate: tenant_id viene da UsageMeterId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    public UsageMeter() {}
    public UsageMeter(Long tenantId, String ym) {
        this.id = new UsageMeterId();
        this.id.setTenantId(tenantId);
        this.id.setYm(ym);
    }

    public UsageMeterId getId() {
        return id;
    }

    public void setId(UsageMeterId id) {
        this.id = id;
    }

    public String getYm() {
        return ym;
    }

    public void setYm(String ym) {
        this.ym = ym;
    }

    public int getCertCount() {
        return certCount;
    }

    public void setCertCount(int certCount) {
        this.certCount = certCount;
    }

    public int getApi_calls() {
        return apiCalls;
    }

    public void setApi_calls(int api_calls) {
        this.apiCalls = api_calls;
    }

    public Long getStorage_bytes() {
        return storageBytes;
    }

    public void setStorage_bytes(Long storage_bytes) {
        this.storageBytes = storage_bytes;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
}
