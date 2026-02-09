package com.example.vericert.dto;

public record VerificationOutcome(boolean ok, String reason, Long tenantId, Long certId) {
    public static VerificationOutcome ok(Long t, Long c){ return new VerificationOutcome(true, null, t, c);}
    public static VerificationOutcome fail(String r){ return new VerificationOutcome(false, r, null, null);}
}