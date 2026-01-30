package com.example.vericert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateEngineConfig {

    @Bean("dbTemplateEngine")
    public org.thymeleaf.TemplateEngine dbTemplateEngine() {
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        var r = new org.thymeleaf.templateresolver.StringTemplateResolver();
        r.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        r.setCacheable(false);          // utile in dev
        r.setName("dbStringResolver");
        engine.setTemplateResolver(r);
        engine.setEnableSpringELCompiler(true);
        return engine;
    }

    // NON definire qui un secondo TemplateEngine chiamato "templateEngine":
    // lascia quello auto-configurato da Spring Boot per le viste MVC.
}