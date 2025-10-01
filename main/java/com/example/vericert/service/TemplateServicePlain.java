package com.example.vericert.service;

import com.example.vericert.domain.Template;
import com.example.vericert.repo.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class TemplateServicePlain {

    private final TemplateRepository templates;

    public TemplateServicePlain(TemplateRepository templates) {
        this.templates = templates;
    }

    public String renderHtml(Long templateId, Map<String,Object> userVars, Map<String,Object> sysVars){
        Template tpl = templates.findById(templateId).orElseThrow();
        String out = tpl.getHtml();
        Map<String,Object> model = new HashMap<>();
        if (userVars != null) model.putAll(userVars);
        if (sysVars != null) model.putAll(sysVars);
        for (var e : model.entrySet()) {
            out = out.replace("{{"+e.getKey()+"}}", Objects.toString(e.getValue(),""));
        }
        return out;
    }
}