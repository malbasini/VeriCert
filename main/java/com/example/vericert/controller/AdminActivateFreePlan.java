package com.example.vericert.controller;


import com.example.vericert.service.AdminPlanDefinitionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminActivateFreePlan {

    private final AdminPlanDefinitionsService service;
    public AdminActivateFreePlan(AdminPlanDefinitionsService service){
        this.service = service;
    }


    @PostMapping("/api/payments/activate-free")
    public ResponseEntity<?> activateFree(@RequestBody activationRequest request)
    {
        service.activatePlan(request.tenantId, "FREE", "MONTHLY", "FREE-" +request.tenantId, "FREE");
        return ResponseEntity.ok(Map.of("ok", true));
    }

    public record activationRequest(Long tenantId){}
}

