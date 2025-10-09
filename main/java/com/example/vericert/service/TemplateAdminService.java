package com.example.vericert.service;

import com.example.vericert.domain.Template;
import com.example.vericert.dto.TemplateUpsert;
import com.example.vericert.repo.TemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

// com.example.vericert.service.TemplateAdminService
@Service
public class TemplateAdminService {
    private final TemplateRepository repo;
    private final TenantService tenantService; // o dal CustomUserDetails

    public TemplateAdminService(TemplateRepository repo, TenantService tenantService) {
        this.repo = repo;
        this.tenantService = tenantService;
    }

    @Transactional
    public Template create(TemplateUpsert req) {
        Long tenantId = tenantService.currentTenantId();
        if (repo.existsByTenantIdAndNameAndVersion(tenantId, req.name(), req.version()))
            throw new IllegalArgumentException("Template con stessa name+version già esiste");

        Template t = new Template();
        t.setTenant(tenantService.ref(tenantId));
        t.setName(req.name());
        t.setVersion(req.version());
        t.setHtml(sanitize(req.html()));
        t.setVariablesJson(normalizeVars(req.variablesJson()));
        t.setActive(false);
        t = repo.save(t);

        if (req.active()) { activate(t.getId()); }
        return t;
    }

    @Transactional
    public Template update(Long id, TemplateUpsert req) {
        Long tenantId = tenantService.currentTenantId();
        Template t = repo.findByTenantIdAndId(tenantId, id).orElseThrow();
        t.setName(req.name());
        t.setVersion(req.version());
        t.setHtml(sanitize(req.html()));
        t.setVariablesJson(normalizeVars(req.variablesJson()));
        t.setActive(false);
        if (req.active()) { // attiva solo alla fine
            repo.save(t);
            activate(id);
        }
        return t;
    }

    @Transactional
    public void activate(Long id) {
        Long tenantId = tenantService.currentTenantId();
        repo.deactivateAll(tenantId);
        Template t = repo.findByTenantIdAndId(tenantId, id).orElseThrow();
        t.setActive(true);
        repo.save(t);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = tenantService.currentTenantId();
        Template t = repo.findByTenantIdAndId(tenantId, id).orElseThrow();
        repo.delete(t);
    }

    private String sanitize(String html) {
        // JSoup relaxed + img/src
        return org.jsoup.Jsoup.clean(
                html,
                org.jsoup.safety.Safelist.relaxed()
                        .addTags("img","style")
                        .addAttributes("img","src","alt","width","height"));
    }

    private String normalizeVars(String vjson) {
        if (vjson == null || vjson.isBlank()) return "[]";
        return vjson;
    }
}
