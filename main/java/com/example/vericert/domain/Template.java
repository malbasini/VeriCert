package com.example.vericert.domain;


import com.example.vericert.tenancy.BaseTenantEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name="template")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Template extends BaseTenantEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false)
    private String name;
    @Column(nullable=false)
    private String version;
    @Lob
    @Column(name = "html", nullable = false, columnDefinition = "TEXT")
    private String html;
    @Column(name="variables_json", nullable=false)
    private String variablesJson;
    @Column(nullable=false, columnDefinition = "boolean default true")
    private boolean active;
    @Column(name="created_at",nullable=false)
    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "tenant_id",nullable = false)
    private Tenant tenant;

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getVariables_json() {
        return variablesJson;
    }

    public void setVariables_json(String variables_json) {
        this.variablesJson = variables_json;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreated_at() {
        return createdAt;
    }

    public void setCreated_at(Instant created_at) {
        this.createdAt = created_at;
    }

}