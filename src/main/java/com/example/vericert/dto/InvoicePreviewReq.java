package com.example.vericert.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record InvoicePreviewReq(
        Map<String, Object> header,
        @NotNull(message = "Le righe sono obbligatorie")
        List<InvoiceLineReq> lines,
        @NotNull(message = "L'iva deve essere valorizzata")
        Integer vatRate
) {}
