package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateInvoiceReq(
                               Long id,
                               @NotBlank
                               String customerName,
                               @NotBlank
                               String customerVat,
                               @NotBlank
                               String customerEmail,
                               Integer vatRate,// default 22
                               @NotNull
                               List<InvoiceLineReq> lines) {}
