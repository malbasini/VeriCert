package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpsertInvoiceReq(
        @NotBlank(message = "Cliente obbligatorio")
        String customerName,
        @NotBlank(message = "Codice fiscale/P.IVA obbligatorio")
        String customerVat,
        @NotBlank(message = "Email cliente obbligatoria")
        String customerEmail,
        @NotNull(message = "L'IVA deve essere valorizzata")
        Integer vatRate,              // default 22 lato service
        Long templateId,              // opzionale, se null prendi active
        @NotBlank(message = "Descrizione obbligatoria")
        String description,// opzionale
        @NotBlank(message = "Indirizzo obbligatorio")
        String customerAddressLine1,
        String customerAddressLine2,
        @NotBlank(message = "Città obbligatoria")
        String customerCity,
        @NotBlank(message = "Provincia obbligatoria")
        String customerProvince,
        @NotBlank(message = "CAP obbligatorio")
        String customerPostalCode,
        @NotBlank(message = "Provincia obbligatoria")
        String customerCountry,
        @NotBlank(message = "PEC obbligatoria")
        String customerPec,
        String customerSdi,
        List<InvoiceLineReq> lines
) {}
