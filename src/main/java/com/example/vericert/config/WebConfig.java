package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage-root}")   // es: /vericert/storage/  (con / finale!)
    private String storageRoot;

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
        String location = (storageRoot.startsWith("/") || storageRoot.startsWith("file:"))
                ? (storageRoot.startsWith("file:") ? storageRoot : "file:" + storageRoot)
                : "file:" + storageRoot;

        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.noCache())   // o maxAge(...) se vuoi caching
                .resourceChain(true);
    }

    // Opzionale: Configura la mappatura della home senza controller
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Value("${vericert.storage.local-path}")   // es: /opt/vericert/storage/
    private String storagePath;

}
