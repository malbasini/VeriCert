package com.example.vericert.controller;


import com.example.vericert.service.AppInfoService;
import com.example.vericert.service.HealthInfoService;
import com.example.vericert.service.LogTailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminHealthApiController {

    private final AppInfoService appInfoService;
    private final LogTailService logTailService;
    private final HealthInfoService healthInfoService;

    public AdminHealthApiController(
            AppInfoService appInfoService,
            LogTailService logTailService,
            HealthInfoService healthInfoService) {

        this.appInfoService = appInfoService;
        this.logTailService = logTailService;
        this.healthInfoService = healthInfoService;
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> health(){
        // stato generale
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> info = healthInfoService.currentHealthInfo();
        String stato = null;
        String statusDisk = null;
        String status = info.get("status").toString(); // "UP", "DOWN", ...
        var details = (Map<String, Object>) info.get("components");
        if (details != null) {
            for (var key : details.keySet()) {
                System.out.println("key: " + key);
                if (key.equals("db")) {
                    var obj = (Map<String, Object>) details.get(key);
                    if (obj != null) {
                        var dbStatus = obj.get("status");
                        if (dbStatus != null) {
                            stato = dbStatus.toString();
                        }
                    }
                } else if (key.equals("diskSpace")) {
                    var obj = (Map<String, Object>) details.get(key);
                    if (obj != null) {
                        map = (Map<String, Object>) obj.get("details");
                        for (var k : obj.keySet()) {
                            if (k.equals("status")) {
                                statusDisk = obj.get(k).toString();
                            }
                        }
                    }
                }
            }
        }

        assert stato != null;
        assert map != null;
        assert statusDisk != null;
        assert status != null;

        //formatto il timestamp
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(Instant.now());

        return ResponseEntity.ok(Map.of(
                "status", status,
                "db",stato,
                "disk",map,
                "disk1",statusDisk,
                "timestamp", formatted,
                "app", appInfoService.currentInfo()  // versione, uptime ecc.
        ));
    }
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> lastLogs() {
        // le ultime ~200 righe
        return ResponseEntity.ok(Map.of(
                "lines", logTailService.tail()
        ));
    }
}
