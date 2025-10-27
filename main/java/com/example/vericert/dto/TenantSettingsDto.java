package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantSettingsDto(
        // Tab "Profilo"
        @NotBlank String displayName,
        @Email String contactEmail,
        String website,

        // Tab "Branding certificati"
        @NotBlank String issuerName,
        @NotBlank String issuerRole,
        String logoUrl,
        String signatureImageUrl,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
        String primaryColor,

        // Template di default per l’emissione
        Long defaultTemplateId
) {}
