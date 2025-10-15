package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import jakarta.validation.constraints.*;

public record CreateReq(

        @NotBlank(message = "Email owner è obbligatoria")
        @Email(message = "Email non valida")
        String ownerEmail,

        String grade,

        @NotNull(message = "Il numero ore è obbligatorio")
        @Positive(message = "Il numero ore deve essere positivo")
        Integer hours,

        @NotBlank(message = "L'intestatario è obbligatorio")
        String ownerName,

        @NotBlank(message = "Il codice corso è obbligatorio")
        String courseCode,

        @NotBlank(message = "Il nome del corso è obbligatorio")
        String courseName) {}