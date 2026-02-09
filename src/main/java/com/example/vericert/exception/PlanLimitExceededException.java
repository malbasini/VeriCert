package com.example.vericert.exception;

import com.example.vericert.enumerazioni.PlanViolationType;

public class PlanLimitExceededException extends RuntimeException {

    private final Long tenantId;
    private final PlanViolationType type;
    public PlanLimitExceededException(Long tenantId, PlanViolationType type, String message) {
        super(message);
        this.tenantId = tenantId;
        this.type = type;
    }
    public Long getTenantId() {
        return tenantId;
    }
    public PlanViolationType getType() {
        return type;
    }
}
