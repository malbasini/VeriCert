package com.example.vericert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

// Piano “master” da cui generi sia l’HTML sia i limiti scritti in tenant_settings
@Entity
@Table(name = "plan_definitions")
public class PlanDefinition {
    @Id
    private Long id;
    @Column(unique = true, nullable = false) // es. "FREE", "PRO"...
    private String code;
    private String name;// "Free", "Pro"...
    @Column(name = "certs_per_month", nullable = false)
    private int certsPerMonth;         // 5, 50, 200...
    @Column(name = "api_calls_per_month")
    private int apiCallsPerMonth;      // 100, 5_000...
    @Column(name = "storage_per_month")
    private long storageMb;            // 100, 10_000...
    @Column(name = "support_priority")
    private String supportPriority;
    // prezzi in centesimi (IVA esclusa o inclusa: decidi una convenzione)
    @Column(name = "price_monthly_cents")
    private long priceMonthlyCents;
    @Column(name = "price_annual_cents")
    private long priceAnnualCents;
    // mappature verso i PSP
    @Column(name = "stripe_price_monthly_id")
    private String stripePriceMonthlyId;
    @Column(name = "stripe_price_annual_id")
    private String stripePriceAnnualId;
    @Column(name = "paypal_plan_monthly_id")
    private String paypalPlanMonthlyId;  // o item/SKU id
    @Column(name = "paypal_plan_annual_id")
    private String paypalPlanAnnualId;
    @Column(name = "vat_code")
    private String vatCode = "22%";
    @Column(name = "annual_discount")
    private String annualDiscount = "20%";

    @UpdateTimestamp
    @Column(name="update_at")
    private Instant updateAt;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCertsPerMonth() {
        return certsPerMonth;
    }

    public void setCertsPerMonth(int certsPerMonth) {
        this.certsPerMonth = certsPerMonth;
    }

    public int getApiCallsPerMonth() {
        return apiCallsPerMonth;
    }

    public void setApiCallsPerMonth(int apiCallsPerMonth) {
        this.apiCallsPerMonth = apiCallsPerMonth;
    }

    public long getStorageMb() {
        return storageMb;
    }

    public void setStorageMb(long storageMb) {
        this.storageMb = storageMb;
    }

    public String isSupportPriority() {
        return supportPriority;
    }

    public void setSupportPriority(String supportPriority) {
        this.supportPriority = supportPriority;
    }

    public long getPriceMonthlyCents() {
        return priceMonthlyCents;
    }

    public void setPriceMonthlyCents(long priceMonthlyCents) {
        this.priceMonthlyCents = priceMonthlyCents;
    }

    public long getPriceAnnualCents() {
        return priceAnnualCents;
    }

    public void setPriceAnnualCents(long priceAnnualCents) {
        this.priceAnnualCents = priceAnnualCents;
    }

    public String getStripePriceMonthlyId() {
        return stripePriceMonthlyId;
    }

    public void setStripePriceMonthlyId(String stripePriceMonthlyId) {
        this.stripePriceMonthlyId = stripePriceMonthlyId;
    }

    public String getStripePriceAnnualId() {
        return stripePriceAnnualId;
    }

    public void setStripePriceAnnualId(String stripePriceAnnualId) {
        this.stripePriceAnnualId = stripePriceAnnualId;
    }

    public String getPaypalPlanMonthlyId() {
        return paypalPlanMonthlyId;
    }

    public void setPaypalPlanMonthlyId(String paypalPlanMonthlyId) {
        this.paypalPlanMonthlyId = paypalPlanMonthlyId;
    }

    public String getPaypalPlanAnnualId() {
        return paypalPlanAnnualId;
    }

    public void setPaypalPlanAnnualId(String paypalPlanAnnualId) {
        this.paypalPlanAnnualId = paypalPlanAnnualId;
    }

    public String getVatCode() {
        return vatCode;
    }

    public void setVatCode(String vatCode) {
        this.vatCode = vatCode;
    }

    public String getAnnualDiscount() {
        return annualDiscount;
    }

    public void setAnnualDiscount(String annualDiscount) {
        this.annualDiscount = annualDiscount;
    }

    public Instant getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Instant updateAt) {
        this.updateAt = updateAt;
    }
}
