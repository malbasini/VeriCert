package com.example.vericert.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signing_key")
public class SigningKeyEntity {

    @Id
    @Column(name = "kid", length = 255)
    private String kid;

    @Lob
    @Column(name = "public_key_pem", columnDefinition = "LONGTEXT", nullable = false)
    private String publicKeyPem;

    @Lob
    @Column(name = "cert_pem", columnDefinition = "LONGTEXT")
    private String certPem;

    @Lob
    @Column(name = "p12_blob", columnDefinition = "LONGBLOB")
    private byte[] p12Blob;

    @Lob
    @Column(name = "p12_password_enc", columnDefinition = "LONGTEXT")
    private String p12PasswordEnc;

    @Column(name = "status", length = 255, nullable = false)
    private String status; // ACTIVE/RETIRED

    @Column(name = "not_before_ts")
    private LocalDateTime notBeforeTs;

    @Column(name = "not_after_ts")
    private LocalDateTime notAfterTs;

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

    public String getCertPem() {
        return certPem;
    }

    public void setCertPem(String certPem) {
        this.certPem = certPem;
    }

    public byte[] getP12Blob() {
        return p12Blob;
    }

    public void setP12Blob(byte[] p12Blob) {
        this.p12Blob = p12Blob;
    }

    public String getP12PasswordEnc() {
        return p12PasswordEnc;
    }

    public void setP12PasswordEnc(String p12PasswordEnc) {
        this.p12PasswordEnc = p12PasswordEnc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getNotBeforeTs() {
        return notBeforeTs;
    }

    public void setNotBeforeTs(LocalDateTime notBeforeTs) {
        this.notBeforeTs = notBeforeTs;
    }

    public LocalDateTime getNotAfterTs() {
        return notAfterTs;
    }

    public void setNotAfterTs(LocalDateTime notAfterTs) {
        this.notAfterTs = notAfterTs;
    }
}

