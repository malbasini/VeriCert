package com.example.vericert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class VericertStorageProperties {

    /**
     * Percorso base sul filesystem dove vengono salvati i PDF / certificati.
     * Esempio: /opt/vericert/storage
     */
    private final Path base;;

    public VericertStorageProperties( @Value("${vercert.storage.root:/data/vericert}") String rootDir) {
        this.base = Paths.get(rootDir).toAbsolutePath().normalize();
    }



    public String getLocalPath() {
        return this.base.getFileName().toString();
    }

}
