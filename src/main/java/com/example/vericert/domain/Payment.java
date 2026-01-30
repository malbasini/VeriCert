package com.example.vericert.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;


@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "certificate_id")
    private Long certificateId; // FK a certificate.id (opzionale)


    @Column(nullable = false, length = 16)
    private String provider; // "STRIPE"


    @Column(name = "checkout_session_id", length = 255, unique = true)
    private String checkoutSessionId; // cs_...


    @Column(name = "provider_intent_id", length = 64)
    private String providerIntentId; // pi_...


    @Column(nullable = false, length = 16)
    private String status; // PENDING | SUCCEEDED | FAILED | CANCELED


    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor; // centesimi


    @Column(nullable = false, length = 8)
    private String currency; // EUR


    @Column(length = 255)
    private String description;


    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;


    @CreationTimestamp
    @Column(name="created_at", updatable=false)
    private Instant createdAt;


    @UpdateTimestamp
    @Column(name="updated_at")
    private Instant updatedAt;

    @Column(name="purchase_email_sent_stripe", nullable=false)
    private boolean purchaseEmailSentStripe;

    @Column(name="renew_email_sent_stripe", nullable=false)
    private boolean renewEmailSentStripe;

    @Column(name="purchase_email_sent_paypal", nullable=false)
    private boolean purchaseEmailSentPaypal;

    @Column(name="renew_email_sent_paypal", nullable=false)
    private boolean renewEmailSentPaypal;

    @Column(name="purchase_invoice_sent_paypal", nullable=false)
    private boolean purchaseInvoiceSentPaypal;

    @Column(name="purchase_invoice_sent_stripe", nullable=false)
    private boolean purchaseInvoiceSentStripe;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }

    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getCertificateId() { return certificateId; }

    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }

    public String getProvider() { return provider; }

    public void setProvider(String provider) { this.provider = provider; }

    public String getCheckoutSessionId() { return checkoutSessionId; }

    public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }

    public String getProviderIntentId() { return providerIntentId; }

    public void setProviderIntentId(String providerIntentId) { this.providerIntentId = providerIntentId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Long getAmountMinor() { return amountMinor; }

    public void setAmountMinor(Long amountMinor) { this.amountMinor = amountMinor; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getIdempotencyKey() { return idempotencyKey; }

    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getCreatedAt() { return createdAt; }

    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPurchaseEmailSentStripe() { return purchaseEmailSentStripe; }
    public void setPurchaseEmailSentStripe(boolean v) { this.purchaseEmailSentStripe = v; }

    public boolean isRenewEmailSentStripe() { return renewEmailSentStripe; }
    public void setRenewEmailSentStripe(boolean v) { this.renewEmailSentStripe = v; }

    public boolean isPurchaseEmailSentPaypal() { return purchaseEmailSentPaypal; }
    public void setPurchaseEmailSentPaypal(boolean v) { this.purchaseEmailSentPaypal = v; }

    public boolean isRenewEmailSentPaypal() { return renewEmailSentPaypal; }
    public void setRenewEmailSentPaypal(boolean v) { this.renewEmailSentPaypal = v; }

    public boolean isPurchaseInvoiceSentPaypal() {
        return purchaseInvoiceSentPaypal;
    }

    public void setPurchaseInvoiceSentPaypal(boolean purchaseInvoiceSentPaypal) {
        this.purchaseInvoiceSentPaypal = purchaseInvoiceSentPaypal;
    }

    public boolean isPurchaseInvoiceSentStripe() {
        return purchaseInvoiceSentStripe;
    }

    public void setPurchaseInvoiceSentStripe(boolean purchaseInvoiceSentStripe) {
        this.purchaseInvoiceSentStripe = purchaseInvoiceSentStripe;
    }
}