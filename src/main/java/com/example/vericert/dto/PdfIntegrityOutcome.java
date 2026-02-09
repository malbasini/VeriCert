package com.example.vericert.dto;

public record PdfIntegrityOutcome(
        boolean ok,
        String message
) {}
