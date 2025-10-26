package com.example.vericert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
        String primaryColor,

        // Template di default per l’emissione
        Long defaultTemplateId
) {}
