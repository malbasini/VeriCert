package com.example.vericert.controller;

import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.service.TenantSigningKeyService;
import com.example.vericert.util.AuthUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class DownloadPublicCertificate{

    private final TenantSigningKeyService tenantSigningKeyService;
    public DownloadPublicCertificate(TenantSigningKeyService tenantSigningKeyService){
        this.tenantSigningKeyService=tenantSigningKeyService;
    }


    @GetMapping("/admin/certificate/download")
    public ResponseEntity<String> get() throws Exception {

        String tenantName = AuthUtil.me().getTenantName();
        Long tenantId = AuthUtil.me().getTenantId();
        SigningKeyEntity sk = tenantSigningKeyService.ensureTenantKey(
                tenantId,
                tenantName
        );

        String certPem = sk.getCertPem();

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=tenant-" + tenantId + "-certificate.crt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(certPem);
    }
}



