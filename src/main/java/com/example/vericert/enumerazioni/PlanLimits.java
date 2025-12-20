package com.example.vericert.enumerazioni;

public enum PlanLimits {

    FREE(5, 100, 100L),
    PRO(100, 50_000, 5_000L),
    BUSINESS(500, 200_000, 25_000L),
    ENTERPRISE(5_000, 1_000_000, 200_000L);

    private final int certsPerMonth;
    private final int apiCallsPerMonth;
    private final long storageMbPerMonth;

    PlanLimits(int certsPerMonth, int apiCallsPerMonth, long storageMbPerMonth) {
        this.certsPerMonth = certsPerMonth;
        this.apiCallsPerMonth = apiCallsPerMonth;
        this.storageMbPerMonth = storageMbPerMonth;
    }

    public int getCertsPerMonth() {
        return certsPerMonth;
    }

    public int getApiCallsPerMonth() {
        return apiCallsPerMonth;
    }

    public long getStorageMbPerMonth() {
        return storageMbPerMonth;
    }

    /**
     * planCode es. "FREE", "PRO", "BUSINESS", "ENTERPRISE"
     */
    public static PlanLimits fromPlanCode(String planCode) {
        return PlanLimits.valueOf(planCode.toUpperCase());
    }
}
