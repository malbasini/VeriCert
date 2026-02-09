package com.example.vericert.dto;

public record VerificationPayload(
        Long tenantId, Long certId, String sha256, long iat, long exp, String jti
) {}