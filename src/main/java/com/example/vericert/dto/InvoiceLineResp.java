package com.example.vericert.dto;

public record InvoiceLineResp(
        Long id,
        String description,
        Integer qty,
        Long unitPriceMinor,
        Integer vatRate,
        Long netMinor,
        Long vatMinor,
        Long grossMinor
) {}