package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import jakarta.validation.constraints.*;

public record CreateReq(

        @NotNull
        @Positive(message = "Il template deve essere positivo")
        Long templateId,

        @NotBlank(message = "L'intestatario è obbligatorio")
        String ownerName,

        @NotBlank(message = "Email owner è obbligatoria")
        @Email(message = "Email non valida")
        String ownerEmail){}
