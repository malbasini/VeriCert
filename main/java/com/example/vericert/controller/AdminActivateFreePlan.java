package com.example.vericert.controller;


import com.example.vericert.service.AdminPlanDefinitionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminActivateFreePlan {

    private final AdminPlanDefinitionsService service;
    public AdminActivateFreePlan(AdminPlanDefinitionsService service){
        this.service = service;
    }
    @PostMapping("/api/payments/activate-free")
    public ResponseEntity<?> activateFree(@RequestParam Long tenantId) { service.activatePlan(tenantId, "FREE", "MONTHLY", "FREE-" +tenantId, "FREE");
        return ResponseEntity.ok(Map.of("ok", true));}
}
