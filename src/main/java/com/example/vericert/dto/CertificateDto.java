package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CertificateDto(
        @NotBlank(message = "Grade obbligatorio")
        String grade,
        @NotBlank(message = "Ore obbligatorie")
        @Positive(message = "Il numero ore deve essere positivo")
        String hours,
        @NotBlank(message = "Owner obbligatorio")
        String ownerName,
        @NotBlank(message = "Codice corso obbligatorio")
        String courseCode,
        @NotBlank(message = "Nome corso obbligatorio")
        String courseName,
        @Email(message = "Email non valida")
        @NotBlank(message = "Owner email obbligatoria")
        String ownerEmail
) {
}
