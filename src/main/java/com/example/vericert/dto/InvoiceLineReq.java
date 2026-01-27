package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InvoiceLineReq(
        @NotBlank(message = "Descrizione obbligatoria")
        String description,
        @NotNull(message= "La quantità deve essere valorizzata e deve essere maggiore di zero")
        @Positive
        Integer qty,
        @NotNull(message = "Il prezzo unitario deve essere valorizzato")
        @Positive
        Long unitPriceMinor) {}
