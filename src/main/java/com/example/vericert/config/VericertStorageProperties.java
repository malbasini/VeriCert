package com.example.vericert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vericert.storage")
public class VericertStorageProperties {

    /**
     * Percorso base sul filesystem dove vengono salvati i PDF / certificati.
     * Esempio: /opt/vericert/storage
     */
    private String localPath;

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
