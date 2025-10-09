package com.example.vericert.dto;

// DTO minimale per la lista
public record TemplateDto(
        Long id,
        String name,
        String version,
        boolean active,
        java.time.Instant updatedAt
) {}