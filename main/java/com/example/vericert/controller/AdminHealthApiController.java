package com.example.vericert.controller;


import com.example.vericert.service.AppInfoService;
import com.example.vericert.service.HealthInfoService;
import com.example.vericert.service.LogTailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminHealthApiController {

    private final AppInfoService appInfoService;
    private final LogTailService logTailService;
    private final HealthInfoService healthInfoService;
    private final DefaultTransactionDefinition defaultTransactionDefinition;

    public AdminHealthApiController(
            AppInfoService appInfoService,
            LogTailService logTailService,
            HealthInfoService healthInfoService,
            DefaultTransactionDefinition defaultTransactionDefinition) {
        this.appInfoService = appInfoService;
        this.logTailService = logTailService;
        this.healthInfoService = healthInfoService;
        this.defaultTransactionDefinition = defaultTransactionDefinition;
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> health() throws JsonProcessingException {
        // stato generale

        // dettagli noti (dipende da cosa hai attivato in actuator)
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> info = healthInfoService.currentHealthInfo();
        String stato = null;
        String status = info.get("status").toString(); // "UP", "DOWN", ...
        var details = (Map<String, Object>) info.get("components");
        if (details != null) {
            for (var key: details.keySet()) {
                System.out.println("key: " + key);
                if (key.equals("db")) {
                    var obj = (Map<String, Object>) details.get(key);
                    if (obj != null) {
                        var dbStatus = obj.get("status");
                        if (dbStatus != null) {
                            stato = dbStatus.toString();
                        }
                    }
                }
                else if (key.equals("diskSpace")) {
                    var obj = (Map<String, Object>) details.get(key);
                    if (obj != null) {
                        map = (Map<String, Object>) obj.get("details");
                    }
                }
            }

        }

        assert stato != null;
        return ResponseEntity.ok(Map.of(
                "status", status,
                "db",stato,
                "disk",map,
                "timestamp", Instant.now().toString(),
                "app", appInfoService.currentInfo()  // versione, uptime ecc.
        ));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lastLogs() {
        // le ultime ~200 righe dalla tua app
        return ResponseEntity.ok(Map.of(
                "lines", logTailService.tail()
        ));
    }
}
