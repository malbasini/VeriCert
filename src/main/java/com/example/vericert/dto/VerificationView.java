package com.example.vericert.dto;

import java.time.Instant;

public class VerificationView {

    private final String code;
    private final String jti;
    private final String kid;
    private final Instant expiresAt;
    private final Long certificateId;
    private final String serial;
    private final String ownerName;
    private java.time.Instant issuedAt;
    private final String pdfUrl;
    private final String compactJws;
    private final String sha256Cached;
    private final Long tenantId;

    public VerificationView(String code,
                            String jti,
                            String kid,
                            Instant expiresAt,
                            Long certificateId,
                            String serial,
                            String ownerName,
                            Instant issuedAt,
                            String pdfUrl,
                            String compactJws,
                            String sha256Cached,
                            Long tenantId) {
        this.code = code;
        this.jti = jti;
        this.kid=kid;
        this.expiresAt = expiresAt;
        this.certificateId = certificateId;
        this.serial = serial;
        this.ownerName = ownerName;
        this.setIssuedAt(issuedAt);
        this.pdfUrl = pdfUrl;
        this.compactJws = compactJws;
        this.sha256Cached = sha256Cached;
        this.tenantId = tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getJti() {
        return jti;
    }

    public String getKid() {
        return kid;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Long getCertificateId() {
        return certificateId;
    }

    public String getSerial() {
        return serial;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getCompactJws() {
        return compactJws;
    }

    public String getSha256Cached() {
        return sha256Cached;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
