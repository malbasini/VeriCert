package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Value("${vercert.storage.root:/data/vericert}")
    private String rootDir;



    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        // Configura il resource handler per i file caricati dagli utenti
        String filesLocation = "file:files/";
        if (!filesLocation.endsWith("/")) filesLocation += "/";

        registry.addResourceHandler("/files/**")
                .addResourceLocations(filesLocation)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true);

        String location = "file:" + Paths.get(rootDir, "storage").toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/storage/**")
                    .addResourceLocations(location);
    }

    // Opzionale: Configura la mappatura della home senza controller
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Value("${vericert.storage.local-path}")   // es: /opt/vericert/storage/
    private String storagePath;

}