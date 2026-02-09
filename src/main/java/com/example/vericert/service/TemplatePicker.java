package com.example.vericert.service;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class TemplatePicker {
    private final TemplateRepository repo;

    public TemplatePicker(TemplateRepository repo) { this.repo = repo; }

    public Template getActiveTemplateOrThrow(Long tenantId) {
        return repo.findByTenantIdAndActiveTrue(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Nessun template ATTIVO per il tenant " + tenantId));
    }
}