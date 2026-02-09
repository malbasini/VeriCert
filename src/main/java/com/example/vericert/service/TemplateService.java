package com.example.vericert.service;

import com.example.vericert.component.SchemaParser;
import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TemplateService {
    private final TemplateRepository templates;
    private final org.thymeleaf.TemplateEngine engine;
    private final SchemaParser parser;
    private final ValidationService validator;

    public TemplateService(TemplateRepository templates,
                           @Qualifier("dbTemplateEngine") org.thymeleaf.TemplateEngine engine,
                           SchemaParser parser,
                           ValidationService validator) {
        this.templates = templates;
        this.engine = engine;
        this.parser = parser;
        this.validator = validator;
    }

    public String renderHtml(Long templateId, Map<String,Object> userVars, Map<String,Object> sysVars){
        Template tpl = templates.getReferenceById(templateId);
        var schema = parser.parse(tpl.getUserVarSchema());
        validator.validateUserVars(userVars, schema);
        var ctx = new org.thymeleaf.context.Context();
        Map<String,Object> model = new HashMap<>();
        if (userVars != null) model.putAll(userVars);
        if (sysVars != null) model.putAll(sysVars);
        ctx.setVariables(model);
        // ATTENZIONE: qui `tpl.getHtml()` è il *contenuto* del template, non il nome file
        return engine.process(tpl.getHtml(), ctx); // usa StringTemplateResolver
    }
}
