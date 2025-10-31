package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

// SigningKey.java
@Entity
@Table(name = "signing_key")
public class SigningKey {
    @Id
    private String kid;

    @Column(name="public_key_pem", nullable=false, columnDefinition="TEXT")
    private String publicKeyPem;

    @Column(nullable=false)
    private String status; // ACTIVE/RETIRED

    @Column(name="not_before_ts")
    private Instant notBeforeTs;

    @Column(name="not_after_ts")
    private Instant notAfterTs;

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getNotBeforeTs() {
        return notBeforeTs;
    }

    public void setNotBeforeTs(Instant notBeforeTs) {
        this.notBeforeTs = notBeforeTs;
    }

    public Instant getNotAfterTs() {
        return notAfterTs;
    }

    public void setNotAfterTs(Instant notAfterTs) {
        this.notAfterTs = notAfterTs;
    }
}