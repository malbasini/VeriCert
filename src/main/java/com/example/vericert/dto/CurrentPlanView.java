package com.example.vericert.dto;

import java.math.BigDecimal;

public class CurrentPlanView {

    private String planCode;
    private String billingCycle;
    private String status;
    private long daysLeft;

    private int usedCerts;
    private Integer certLimit;

    private int usedApi;
    private Integer apiLimit;

    private BigDecimal usedStorage;
    private BigDecimal storageLimit;

    private double certsUsageRatio; // 0.0 - 1.0
    private double apiUsageRatio;

    public CurrentPlanView(String planCode,
                           String billingCycle,
                           String status,
                           long daysLeft,
                           int usedCerts,
                           Integer certLimit,
                           int usedApi,
                           Integer apiLimit,
                           BigDecimal usedStorage,
                           BigDecimal storageLimit) {
        this.planCode = planCode;
        this.billingCycle = billingCycle;
        this.status = status;
        this.daysLeft = daysLeft;
        this.usedCerts = usedCerts;
        this.certLimit = certLimit;
        this.usedApi = usedApi;
        this.apiLimit = apiLimit;
        this.setUsedStorage(usedStorage);
        this.setStorageLimit(storageLimit);

        this.certsUsageRatio = (certLimit == null || certLimit == 0)
                ? 0.0
                : (double) usedCerts / certLimit;

        this.apiUsageRatio = (apiLimit == null || apiLimit == 0)
                ? 0.0
                : (double) usedApi / apiLimit;
    }
    public String getPlanCode() { return planCode; }
    public String getBillingCycle() { return billingCycle; }
    public String getStatus() { return status; }
    public long getDaysLeft() { return daysLeft; }
    public int getUsedCerts() { return usedCerts; }
    public Integer getCertLimit() { return certLimit; }
    public int getUsedApi() { return usedApi; }
    public Integer getApiLimit() { return apiLimit; }
    public double getCertsUsageRatio() { return certsUsageRatio; }
    public double getApiUsageRatio() { return apiUsageRatio; }

    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(status);
    }

    public boolean isExpiringSoon() {
        return !isExpired() && daysLeft >= 0 && daysLeft <= 5;
    }

    public boolean isCertsAlmostExceeded() {
        return certLimit != null && certLimit > 0 && certsUsageRatio >= 0.8;
    }

    public boolean isApiAlmostExceeded() {
        return apiLimit != null && apiLimit > 0 && apiUsageRatio >= 0.8;
    }

    public BigDecimal getUsedStorage() {
        return usedStorage;
    }

    public void setUsedStorage(BigDecimal usedStorage) {
        this.usedStorage = usedStorage;
    }

    public BigDecimal getStorageLimit() {
        return storageLimit;
    }

    public void setStorageLimit(BigDecimal storageLimit) {
        this.storageLimit = storageLimit;
    }
}
