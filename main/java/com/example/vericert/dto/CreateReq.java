package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import jakarta.validation.constraints.*;

public record CreateReq(
        @NotNull(message = "Il template è obbligatorio")
        @Positive(message = "L'id del template deve essere positivo")
        Long templateId,

        @NotEmpty(message = "vars è obbligatorio")
        Map<String, Object> vars,

        @NotBlank(message = "Nome owner è obbligatorio")
        String ownerName,

        @NotBlank(message = "Email owner è obbligatoria")
        @Email(message = "Email non valida")
        String ownerEmail,

        @NotBlank(message = "Codice corso è obbligatorio")
        String courseCode
) {}