package com.example.vericert.service;

import com.example.vericert.config.VericertProps;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.util.QrUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;


@Service
public class SystemVarsBuilder {

    private final VericertProps props;
    private final TenantSettingsService tenantSettingsService;
    public SystemVarsBuilder(TenantRepository tenantRepo,
                             VericertProps props,
                             TenantSettingsService tenantSettingsService) {
        this.props = props;
        this.tenantSettingsService = tenantSettingsService;
    }

    public Map<String, Object> buildPreviewVars() {
        String serial = UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase();
        String code = CertificateService.randomCode(24);
        String verifyUrl = props.getPublicBaseUrlVerify() + "/v/" + code;
        byte[] qr = QrUtil.png(verifyUrl, 300);
        String qrBase64 = Base64.getEncoder().encodeToString(qr);
        Map<String, Object> vars = tenantSettingsService.buildBaseSysVarsForTenant(currentTenantId());
        vars.put("serial", serial);
        vars.put("verifyUrl", verifyUrl);
        vars.put("qrBase64", qrBase64);
        vars.put("issuedAt", Instant.now());
        return vars;
    }
    private Long currentTenantId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        var user = (com.example.vericert.service.CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }
}
