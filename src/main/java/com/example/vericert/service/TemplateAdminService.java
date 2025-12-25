package com.example.vericert.service;

import com.example.vericert.domain.Template;
import com.example.vericert.dto.TemplateUpsert;
import com.example.vericert.repo.TemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Template create(TemplateUpsert req) throws Exception {
        Long tenantId = tenantService.currentTenantId();
        if (repo.existsByTenantIdAndNameAndVersion(tenantId, req.name(), req.version()))
            throw new IllegalArgumentException("Template con stessa name+version già esiste");

        Template t = new Template();
        t.setTenant(tenantService.ref(tenantId));
        t.setName(req.name());
        t.setVersion(req.version());
        t.setHtml(req.html());
        t.setUserVarSchema(req.variablesUserJson());
        t.setSysVarsSchema(req.systemsVariables());
        t.setActive(req.active());
        t = repo.save(t);
        return t;
    }

    @Transactional
    public Template update(Long id, TemplateUpsert req) throws Exception {
        Long tenantId = tenantService.currentTenantId();
        Template t = repo.findByTenantIdAndId(tenantId, id).orElseThrow();
        t.setName(req.name());
        t.setVersion(req.version());
        t.setHtml(req.html());
        t.setUserVarSchema(req.variablesUserJson());
        t.setSysVarsSchema(req.systemsVariables());
        t.setActive(req.active());
        repo.save(t);
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
        Template t = repo.findById(id).orElseThrow();
        repo.delete(t);
    }
    private String normalizeVars(String vjson) {
        if (vjson == null || vjson.isBlank()) return "[]";
        return vjson;
    }
    @Transactional
    public void deactivateAll(Long tenantId,Template template) {
        List<Template> list = repo.findAll();
        for (Template t : list) {
            if (t.getTenant().getId().equals(tenantId)) t.setActive(false);
        }
        repo.saveAll(list);
        template.setActive(true);
        repo.save(template);
    }

    @Transactional
    public void controlsValidity(Long tenantId, Template t) {
        List<Template> list = repo.findAll();
        boolean valid = false;
        for (Template v : list) {
            if (v.getTenant().getId().equals(tenantId)){
                if(v.isActive()) {
                    valid = true;
                    break;
                }
            }
        }
        if (!valid) {
            t.setActive(true);
            repo.save(t);
            throw new IllegalStateException("Almeno un template deve essere attivo");
        }

    }
}
