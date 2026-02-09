package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReq(

        @NotNull
        @Positive(message = "Il template deve essere positivo")
        Long templateId,

        @NotBlank(message = "L'intestatario è obbligatorio")
        String ownerName,

        @NotBlank(message = "Email owner è obbligatoria")
        @Email(message = "Email non valida")
        String ownerEmail){}
