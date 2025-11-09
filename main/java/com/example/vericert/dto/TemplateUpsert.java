package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.minidev.json.annotate.JsonIgnore;

public record TemplateUpsert(
        Long id,
        @NotBlank(message = "Nome del template obbligatorio") String name,
        @NotBlank(message="Versione obbligatoria") String version,
        @NotBlank(message = "Il campo html è obbligatorio") String html,
        String variablesUserJson,
        String systemsVariables,
        boolean active,
        String captchaToken
) {}