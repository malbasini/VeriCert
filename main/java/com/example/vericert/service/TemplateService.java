package com.example.vericert.service;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.HashMap;
import java.util.Map;

@Service
public class TemplateService {

    private final TemplateRepository templates;
    private final TemplateEngine stringTemplateEngine;

    public TemplateService(TemplateRepository templates, @Qualifier("dbTemplateEngine") TemplateEngine stringTemplateEngine) {
        this.templates = templates;
        this.stringTemplateEngine = stringTemplateEngine;
    }

    public String renderHtml(Long templateId, Map<String, Object> userVars, Map<String, Object> sysVars) {
        Template tpl = templates.findById(templateId).orElseThrow();
        var ctx = new Context();
        Map<String,Object> model = new HashMap<>();
        if (userVars != null) model.putAll(userVars);
        if (sysVars  != null) model.putAll(sysVars);
        ctx.setVariables(model);
        return stringTemplateEngine.process(tpl.getHtml(), ctx); // processa LA STRINGA
    }
}
