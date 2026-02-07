package com.example.vericert.domain;

import com.example.vericert.enumerazioni.Stato;
import com.example.vericert.tenancy.BaseTenantEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name="certificate")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Certificate extends BaseTenantEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=64)
    private String serial;
    @Column(name = "owner_name", nullable=false)
    private String ownerName;
    @Column(name = "owner_email", nullable=false)
    private String ownerEmail;
    @Column(name = "pdf_url", nullable=false)
    private String pdfUrl;
    @Column(nullable=false, length=64)
    private String sha256;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable=false)
    private Stato status = Stato.ISSUED;
    @Column(name="issued_at",nullable=false)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant issuedAt = Instant.now();
    @Column(name="revoked_reason")
    private String revokedReason;
    @Column(name="revoked_at")
    private Instant revokedAt;
    @Column(name="signing_kid")
    private String kid;
    @Column(name="user_vars_json",columnDefinition="JSON")
    private String userVarsJson;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="tenant_id", nullable=false)
    private Tenant tenant;

    // getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Stato getStatus() {
        return status;
    }

    public Stato setStatus(Stato status) {
        this.status = status;
        return status;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getUserVarsJson() {
        return userVarsJson;
    }

    public void setUserVarsJson(String userVarsJson) {
        this.userVarsJson = userVarsJson;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }
}