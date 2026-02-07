package com.example.vericert.dto;

public record InvoiceLineView(
        String description,
        int qty,
        long unitPriceMinor,
        long netMinor,
        long vatMinor,
        long grossMinor
) {}