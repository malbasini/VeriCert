package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/static/**") // Consente solo le risorse statiche
                .allowedOrigins("http://localhost:8080") // Permette solo dal dominio specificato
                .allowedMethods("GET") // Solo richieste GET
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        // punta alla cartella *assoluta* sul filesystem. NOTA: "file:" + trailing slash
        // ATTENZIONE: serve "file:" e una directory reale, senza pattern
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:/vericert/storage/") // <- con slash finale
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

    }

    // Opzionale: Configura la mappatura della home senza controller
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
    }

    @Value("${vericert.storage.local-path}")   // es: /opt/vericert/storage/
    private String storagePath;

}
