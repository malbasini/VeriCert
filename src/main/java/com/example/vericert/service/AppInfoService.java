package com.example.vericert.service;

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AppInfoService {

    private final BuildProperties buildPropertiesOrNull;
    private final long startTimeMs;

    // BuildProperties è opzionale
    public AppInfoService(org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropsProvider) {
        this.buildPropertiesOrNull = buildPropsProvider.getIfAvailable();
        this.startTimeMs = ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    public Map<String,Object> currentInfo() {
        Instant startedAt = Instant.ofEpochMilli(startTimeMs);
        Duration up = Duration.between(startedAt, Instant.now());

        Map<String,Object> info = new HashMap<>();
        if (buildPropertiesOrNull != null) {
            info.put("name",    buildPropertiesOrNull.getName());
            info.put("version", buildPropertiesOrNull.getVersion());
            info.put("time",    buildPropertiesOrNull.getTime()); // Instant del build
        } else {
            // fallback se non hai ancora configurato build-info
            info.put("name",    "vericert");
            info.put("version", "dev");
            info.put("time",    Instant.EPOCH);
        }

        info.put("uptimeSeconds", up.toSeconds());
        return info;
    }
}
