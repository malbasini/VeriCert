package com.example.vericert.dto;

public record GenerateTenantKeyRequest(
        Long tenantId,
        String tenantSlug
) {}
