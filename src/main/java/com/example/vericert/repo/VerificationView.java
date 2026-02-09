package com.example.vericert.repo;

public interface VerificationView {
    String getCode();
    String getSerial();
    String getOwnerName();
    String getCourseCode();
    java.time.Instant getIssuedAt();
    java.time.Instant getRevokedAt();
    String getRevokedReason();
    Long getTenantId();
}