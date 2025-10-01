package com.example.vericert.domain;
import jakarta.persistence.*;
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
}