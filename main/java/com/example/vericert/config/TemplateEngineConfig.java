package com.example.vericert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
@Primary
@Configuration
public class TemplateEngineConfig {

    @Bean
    public org.thymeleaf.TemplateEngine dbTemplateEngine() {
        var resolver = new org.thymeleaf.templateresolver.StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        var engine = new org.thymeleaf.TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }



}