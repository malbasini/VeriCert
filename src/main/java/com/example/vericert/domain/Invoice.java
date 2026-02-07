package com.example.vericert.domain;

import com.example.vericert.enumerazioni.InvoiceStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices",
        indexes = {
                @Index(name = "ix_invoices_tenant_year_number", columnList = "tenant_id, issue_year, number_seq", unique = true),
                @Index(name = "ix_invoices_public_code", columnList = "public_code", unique = true)
        })
public class Invoice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="tenant_id", nullable=false)
    private Long tenantId;

    @Column(name="public_code", nullable=false, length=16, unique = true)
    private String publicCode; // INV-8F3K2P

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    // Numerazione (per tenant + anno)
    @Column(name="issue_year")
    private Integer issueYear;

    @Column(name="number_seq")
    private Long numberSeq; // 1..N per anno

    @Column(name="number_display", length=32)
    private String numberDisplay; // es "2026/000001"

    @Column(name="issued_at")
    private Instant issuedAt;

    // Cliente (minimal)
    @Column(name="customer_name", length=120)
    private String customerName;

    @Column(name="customer_vat", length=32)
    private String customerVat;

    @Column(name="customer_email", length=160)
    private String customerEmail;

    @Column(name="customer_address_line1", length=160)
    private String customerAddressLine1;

    @Column(name="customer_address_line2", length=160)
    private String customerAddressLine2;

    @Column(name="customer_city", length=80)
    private String customerCity;

    @Column(name="customer_province", length=8)
    private String customerProvince;

    @Column(name="customer_postal_code", length=16)
    private String customerPostalCode;

    @Column(name="customer_country", length=2)
    private String customerCountry;

    @Column(name="customer_pec", length=160)
    private String customerPec;

    @Column(name="customer_sdi", length=16)
    private String customerSdi;


    // Importi in centesimi
    @Column(name="currency", nullable=false, length=3)
    private String currency = "EUR";

    @Column(name="vat_rate", nullable=false)
    private Integer vatRate = 22;

    @Column(name="net_total_minor", nullable=false)
    private Long netTotalMinor = 0L;

    @Column(name="vat_total_minor", nullable=false)
    private Long vatTotalMinor = 0L;

    @Column(name="gross_total_minor", nullable=false)
    private Long grossTotalMinor = 0L;

    // PDF salvato in DB
    @Lob
    @Column(name="pdf_blob", columnDefinition="LONGBLOB")
    private byte[] pdfBlob;

    // Hash del PDF (hex)
    @Column(name="pdf_sha256",columnDefinition="VARCHAR(255)")
    private String pdfSha256;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Column(name="template_id", nullable=false)
    private long templateId;

    @Column(name="description", nullable=false)
    private String description;

    @Column(name="invoice_code", nullable=false)
    private String invoiceCode;

    @Column(name="invoice_saved", nullable=false)
    private boolean invoiceSave;

    @PrePersist
    @PreUpdate
    void prePersist() {
        Instant now = Instant.now();
        setCreatedAt(now);
        setUpdatedAt(now);
        if (issuedAt != null) {
            // Converte l'Instant in una data leggibile per estrarre l'anno
            // Puoi usare ZoneId.systemDefault() o specificare una zona (es. UTC)
            this.issueYear = ZonedDateTime.ofInstant(issuedAt, ZoneId.systemDefault()).getYear();
        }
    }
    // helper
    public void addLine(InvoiceLine l) {
        l.setInvoice(this);
        getLines().add(l);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public void setPublicCode(String publicCode) {
        this.publicCode = publicCode;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Integer getIssueYear() {
        return issueYear;
    }

    public void setIssueYear(Integer issueYear) {
        this.issueYear = issueYear;
    }

    public Long getNumberSeq() {
        return numberSeq;
    }

    public void setNumberSeq(Long numberSeq) {
        this.numberSeq = numberSeq;
    }

    public String getNumberDisplay() {
        return numberDisplay;
    }

    public void setNumberDisplay(String numberDisplay) {
        this.numberDisplay = numberDisplay;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerVat() {
        return customerVat;
    }

    public void setCustomerVat(String customerVat) {
        this.customerVat = customerVat;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getVatRate() {
        return vatRate;
    }

    public void setVatRate(Integer vatRate) {
        this.vatRate = vatRate;
    }

    public Long getNetTotalMinor() {
        return netTotalMinor;
    }

    public void setNetTotalMinor(Long netTotalMinor) {
        this.netTotalMinor = netTotalMinor;
    }

    public Long getVatTotalMinor() {
        return vatTotalMinor;
    }

    public void setVatTotalMinor(Long vatTotalMinor) {
        this.vatTotalMinor = vatTotalMinor;
    }

    public Long getGrossTotalMinor() {
        return grossTotalMinor;
    }

    public void setGrossTotalMinor(Long grossTotalMinor) {
        this.grossTotalMinor = grossTotalMinor;
    }

    public byte[] getPdfBlob() {
        return pdfBlob;
    }

    public void setPdfBlob(byte[] pdfBlob) {
        this.pdfBlob = pdfBlob;
    }

    public String getPdfSha256() {
        return pdfSha256;
    }

    public void setPdfSha256(String pdfSha256) {
        this.pdfSha256 = pdfSha256;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public boolean isInvoiceSave() {
        return invoiceSave;
    }

    public void setInvoiceSave(boolean invoiceSave) {
        this.invoiceSave = invoiceSave;
    }

    public String getCustomerAddressLine1() {
        return customerAddressLine1;
    }

    public void setCustomerAddressLine1(String customerAddressLine1) {
        this.customerAddressLine1 = customerAddressLine1;
    }

    public String getCustomerAddressLine2() {
        return customerAddressLine2;
    }

    public void setCustomerAddressLine2(String customerAddressLine2) {
        this.customerAddressLine2 = customerAddressLine2;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerProvince() {
        return customerProvince;
    }

    public void setCustomerProvince(String customerProvince) {
        this.customerProvince = customerProvince;
    }

    public String getCustomerPostalCode() {
        return customerPostalCode;
    }

    public void setCustomerPostalCode(String customerPostalCode) {
        this.customerPostalCode = customerPostalCode;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }

    public String getCustomerPec() {
        return customerPec;
    }

    public void setCustomerPec(String customerPec) {
        this.customerPec = customerPec;
    }

    public String getCustomerSdi() {
        return customerSdi;
    }

    public void setCustomerSdi(String customerSdi) {
        this.customerSdi = customerSdi;
    }
}
