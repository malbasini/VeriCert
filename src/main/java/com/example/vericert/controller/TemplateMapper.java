package com.example.vericert.controller;

import com.example.vericert.dto.TemplateDto;

public final class TemplateMapper {
    private TemplateMapper() {}
    public static TemplateDto toDto(com.example.vericert.domain.Template t) {
        return new TemplateDto(
                t.getId(),
                t.getName(),
                t.getVersion(),
                t.isActive(),
                t.getUpdatedAt()
        );
    }
}