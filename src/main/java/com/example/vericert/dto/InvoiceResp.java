package com.example.vericert.dto;

import java.util.List;

public record InvoiceResp(
        Long id,
        Long tenantId,
        String publicCode,
        String status,
        Integer issueYear,
        Long numberSeq,
        String numberDisplay,
        String issuedAt,
        String customerName,
        String customerVat,
        String description,
        String customerEmail,
        String customerAddressLine1,
        String customerAddressLine2,
        String customerCity,
        String customerProvince,
        String customerPostalCode,
        String customerCountry,
        String customerPec,
        String customerSdi,
        String currency,
        Integer vatRate,
        Long netTotalMinor,
        Long vatTotalMinor,
        Long grossTotalMinor,
        Long templateId,
        String serial,
        List<InvoiceLineResp> lines
) {
}