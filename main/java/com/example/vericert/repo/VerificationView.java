package com.example.vericert.repo;

public interface VerificationView {
    String getCode();
    String getJti();
    String getKid();
    java.time.Instant expiresAt();
    Long getCertificateId();
    String getSerial();
    String getOwnerName();
    java.time.Instant getIssuedAt();
    String getPdfUrl();
    String getCompactJws();
    String getSha256Cached();
    Long getTenantId();
}
