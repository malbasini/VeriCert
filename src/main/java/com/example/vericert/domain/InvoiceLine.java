package com.example.vericert.domain;

import jakarta.persistence.*;

@Entity
@Table(name="invoice_lines",
        indexes = @Index(name="ix_invoice_lines_invoice_id", columnList="invoice_id"))
public class InvoiceLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_id", nullable=false)
    private Invoice invoice;

    @Column(nullable=false, length=255)
    private String description;

    @Column(nullable=false)
    private Integer qty = 1;

    @Column(name="unit_price_minor", nullable=false)
    private Long unitPriceMinor = 0L;

    @Column(name="net_minor", nullable=false)
    private Long netMinor = 0L;

    @Column(name="vat_rate", nullable=false)
    private Integer vatRate = 22;

    @Column(name="vat_minor", nullable=false)
    private Long vatMinor = 0L;

    @Column(name="gross_minor", nullable=false)
    private Long grossMinor = 0L;

    @Column(name="sort_order")
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Long getUnitPriceMinor() {
        return unitPriceMinor;
    }

    public void setUnitPriceMinor(Long unitPriceMinor) {
        this.unitPriceMinor = unitPriceMinor;
    }

    public Long getNetMinor() {
        return netMinor;
    }

    public void setNetMinor(Long netMinor) {
        this.netMinor = netMinor;
    }

    public Integer getVatRate() {
        return vatRate;
    }

    public void setVatRate(Integer vatRate) {
        this.vatRate = vatRate;
    }

    public Long getVatMinor() {
        return vatMinor;
    }

    public void setVatMinor(Long vatMinor) {
        this.vatMinor = vatMinor;
    }

    public Long getGrossMinor() {
        return grossMinor;
    }

    public void setGrossMinor(Long grossMinor) {
        this.grossMinor = grossMinor;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
