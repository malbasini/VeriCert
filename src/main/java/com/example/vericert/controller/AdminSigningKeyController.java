package com.example.vericert.controller;

import com.example.vericert.dto.GenerateTenantKeyRequest;
import com.example.vericert.repo.TenantSigningKeyRepository;
import com.example.vericert.service.TenantSigningKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/keys")
public class AdminSigningKeyController {

    private final TenantSigningKeyService tenantSigningKeyService;
    private final TenantSigningKeyRepository tenantSigningKeyRepo;

    public AdminSigningKeyController(TenantSigningKeyService tenantSigningKeyService,
                                     TenantSigningKeyRepository tenantSigningKeyRepo) {
        this.tenantSigningKeyService = tenantSigningKeyService;
        this.tenantSigningKeyRepo = tenantSigningKeyRepo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{tenant}/generate")
    public ResponseEntity<?> generateForTenant(@RequestBody GenerateTenantKeyRequest req) throws Exception {
        var sk = tenantSigningKeyService.ensureTenantKey(req.tenantId(), req.tenantSlug());
        return ResponseEntity.ok(new KeySummary(sk.getKid(), sk.getStatus(), sk.getNotBeforeTs(), sk.getNotAfterTs()));
    }

    public record KeySummary(String kid, String status, Object notBefore, Object notAfter) {}

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/rotate")
    public ResponseEntity<?> rotateForTenant(@RequestBody GenerateTenantKeyRequest req) throws Exception {
        var sk = tenantSigningKeyService.rotateTenantKey(req.tenantId(), req.tenantSlug());
        return ResponseEntity.ok(new KeySummary(sk.getKid(), sk.getStatus(), sk.getNotBeforeTs(), sk.getNotAfterTs()));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<?> getTenantKey(@PathVariable Long tenantId) {
        return tenantSigningKeyRepo.findActiveSigningKeyByTenant(tenantId)
                .<ResponseEntity<?>>map(sk -> ResponseEntity.ok(new KeySummary(sk.getKid(), sk.getStatus(), sk.getNotBeforeTs(), sk.getNotAfterTs())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
