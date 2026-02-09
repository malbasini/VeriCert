package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name="verification_token")
public class VerificationToken {
    @Id
    @Column(length=24)
    private String code;
    @Column(name="certificate_id")
    private Long certificateId;

    @Column(name="created_at")
    private Instant createdAt = Instant.now();

    @Column(name="expires_at")
    private Instant expiresAt;

    @Column(name="kid")
    private String kid;
    @Column(name="jti")
    private String jti;
    @Column(name="sha256_cached")
    private String sha256Cached;// opzionale
    @Column(name="compact_jws",columnDefinition = "TEXT")
    private String compactJws;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getSha256Cached() {
        return sha256Cached;
    }

    public void setSha256Cached(String sha256Cached) {
        this.sha256Cached = sha256Cached;
    }

    public String getCompactJws() {
        return compactJws;
    }

    public void setCompactJws(String compactJws) {
        this.compactJws = compactJws;
    }
}