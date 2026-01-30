package com.example.vericert.domain;

import com.example.vericert.enumerazioni.PlanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tenant_settings")
public class TenantSettings {
    @Id
    @Column(name = "tenant_id")
    private Long tenantId;
    @Column(name = "email",nullable = false)
    private String email;
    @Column(name = "plan_code",nullable = false)
    private String planCode;
    @Column(name = "billing_cycle",nullable = false)
    private String billingCycle;
    @Column(name = "current_period_start",nullable = false)
    private Instant currentPeriodStart;
    @Column(name = "current_period_end",nullable = false)
    private Instant currentPeriodEnd;
    @Column(name = "certs_per_month")
    private int certsPerMonth;
    @Column(name = "api_call_per_month")
    private int apiCallPerMonth;
    @Column(name = "storage_mb")
    private BigDecimal storageMb;
    @Column(name = "support")
    private String support;
    @Column(name = "provider")
    private String provider;
    @Column(name = "checkout_session_id")
    private String checkoutSessionId;
    @Column(name = "subscription_id")
    private String subscriptionId;
    @Column(name = "last_invoice_id")
    private String lastInvoiceId;
    @Column(name = "status",nullable = false)
    private String status;
    @Column(name = "notify_expiring",nullable = true)
    private boolean notifyExpiring = false;
    // JSON salvato come testo normale -> NIENTE @Lob
    @Column(
            name = "json_settings",
            nullable = false,
            columnDefinition = "JSON"
    )
    private String jsonSettings;
    // Lasciamo che MySQL lo mantenga con DEFAULT ... ON UPDATE ...
    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Instant updatedAt;

    public TenantSettings() {}

    public TenantSettings(Long tenantId, String jsonSettings) {
        this.setTenantId(tenantId);
        this.setJsonSettings(jsonSettings);
    }

    public PlanStatus getStatusEnum() {
        return status == null ? null : PlanStatus.valueOf(status);
    }

    public void setStatusEnum(PlanStatus s) {
        this.status = s == null ? null : s.name();
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public int getCertsPerMonth() {
        return certsPerMonth;
    }

    public void setCertsPerMonth(int certsPerMonth) {
        this.certsPerMonth = certsPerMonth;
    }

    public int getApiCallPerMonth() {
        return apiCallPerMonth;
    }

    public void setApiCallPerMonth(int apiCallPerMonth) {
        this.apiCallPerMonth = apiCallPerMonth;
    }

    public BigDecimal getStorageMb() {
        return storageMb;
    }

    public void setStorageMb(BigDecimal storageMb) {
        this.storageMb = storageMb;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public void setCheckoutSessionId(String checkoutSessionId) {
        this.checkoutSessionId = checkoutSessionId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getLastInvoiceId() {
        return lastInvoiceId;
    }

    public void setLastInvoiceId(String lastInvoiceId) {
        this.lastInvoiceId = lastInvoiceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJsonSettings() {
        return jsonSettings;
    }

    public void setJsonSettings(String jsonSettings) {
        this.jsonSettings = jsonSettings;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isNotifyExpiring() {
        return notifyExpiring;
    }

    public void setNotifyExpiring(boolean notifyExpriring) {
        this.notifyExpiring = notifyExpriring;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
