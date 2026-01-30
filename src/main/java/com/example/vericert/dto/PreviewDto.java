package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PreviewDto(

        String grade,

        @NotNull(message = "Il numero ore è obbligatorio")
        @Positive(message = "Il numero ore deve essere positivo")
        Integer hours,

        @NotBlank(message = "L'intestatario è obbligatorio")
        String ownerName,

        @NotBlank(message = "Email intestatario obbligatoria")
        @Email(message = "Email non valida")
        String ownerEmail,

        @NotBlank(message = "Il codice corso è obbligatorio")
        String courseCode,

        @NotBlank(message = "Il nome del corso è obbligatorio")
        String courseName) {
}
