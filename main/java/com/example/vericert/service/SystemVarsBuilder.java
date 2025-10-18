package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.util.QrUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class SystemVarsBuilder {

    private final VericertProps props;

    public SystemVarsBuilder(TenantRepository tenantRepo, VericertProps props) {
        this.props = props;
    }

    public Map<String, Object> buildPreviewVars() {
        String serial = UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();
        String code = CertificateService.randomCode(24);
        String verifyUrl = props.getPublicBaseUrl() + "/v/" + code;
        byte[] qr = QrUtil.png(verifyUrl, 300);
        String qrBase64 = Base64.getEncoder().encodeToString(qr);
        java.util.Map<String, Object> vars = new HashMap<>();
        vars.put("serial", serial);
        vars.put("verifyUrl", verifyUrl);
        vars.put("qrBase64", qrBase64);
        vars.put("issuedAt", Instant.now());
        return vars;
    }
}
