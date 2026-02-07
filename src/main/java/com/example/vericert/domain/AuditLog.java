package com.example.vericert.domain;


import com.example.vericert.tenancy.BaseTenantEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="audit_log")
public class AuditLog extends BaseTenantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="actor", nullable=false)
    private String actor;
    @Column(name="action", nullable=false)
    private String action;
    @Column(name="entity", nullable=false)
    private String entity;
    @Column(name="entity_id", nullable=false)
    private String entityId;
    @Column(name="ts",nullable=false)
    private Instant ts = Instant.now();
    @Column(name="payload")
    private String payload;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false,insertable = false,updatable = false)
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

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
