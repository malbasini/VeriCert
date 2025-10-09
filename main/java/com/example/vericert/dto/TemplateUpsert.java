package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateUpsert(
        @NotBlank(message = "Nome del template obbligatorio") String name,
        @NotBlank(message="Versione obbligatoria") String version,
        @NotBlank(message = "Il campo html è obbligatorio") String html,
        String variablesJson, // opz
        boolean active
) {}