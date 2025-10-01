package com.example.vericert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Configuration
public class TemplateEngineConfig {

    @Bean
    @Primary
    public TemplateEngine stringTemplateEngine() {
        TemplateEngine engine = new TemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML"); // puoi usare anche "HTML5"
        resolver.setCacheable(false);     // utile in sviluppo, evita cache
        engine.setTemplateResolver(resolver);
        return engine;
    }


    // Opzionale: Configura la mappatura della home senza controller
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
    }
}