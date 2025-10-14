package com.example.vericert.domain;


import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name="template")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Template {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="tenant_id", nullable=false)
    private Tenant tenant;

    @Column(nullable=false, length=120)
    private String name;

    @Column(nullable=false, length=20)
    private String version;      // es. "v1", "2025-10-06.1"

    @Lob @Column(columnDefinition="TEXT", nullable=false)
    private String html;

    @Column(name="variables_json", columnDefinition="JSON")
    private String variablesJson; // ["ownerName","courseName",...]

    @Column(name="user_vars_schema", columnDefinition="JSON")
    private String userVarSchema;

    @Column(name="sys_vars_schema", columnDefinition="JSON")
    private String sysVarsScahema;

    @Column(nullable=false)
    private boolean active;

    @Column(name="created_at") private Instant createdAt = Instant.now();
    @Column(name="updated_at") private Instant updatedAt = Instant.now();

    @PreUpdate void touch(){ this.setUpdatedAt(Instant.now()); }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
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

    public String getVariablesJson() {
        return variablesJson;
    }

    public void setVariablesJson(String variablesJson) {
        this.variablesJson = variablesJson;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserVarSchema() {
        return userVarSchema;
    }

    public void setUserVarSchema(String userVarSchema) {
        this.userVarSchema = userVarSchema;
    }

    public String getSysVarsScahema() {
        return sysVarsScahema;
    }

    public void setSysVarsScahema(String sysVarsScahema) {
        this.sysVarsScahema = sysVarsScahema;
    }
}
