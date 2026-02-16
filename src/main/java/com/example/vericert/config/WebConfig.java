package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${vercert.storage.root:storage}")
    private String rootDir;

    // true in prod (rootDir=/data/vericert -> serve /storage), false in dev (rootDir=storage -> già dentro)
    @Value("${vercert.storage.append-storage-segment:false}")
    private boolean appendStorageSegment;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path base = Paths.get(rootDir).toAbsolutePath().normalize();

        if (appendStorageSegment) {
            base = base.resolve("storage").normalize();
        }

        String storageLocation = "file:" + base + "/";

        registry.addResourceHandler("/storage/**")
                .addResourceLocations(storageLocation)
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }
}