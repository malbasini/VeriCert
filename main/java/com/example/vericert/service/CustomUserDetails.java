package com.example.vericert.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class CustomUserDetails extends User {

    private final Long tenantId;
    private final String tenantName;

    public CustomUserDetails(
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            Long tenantId,
            String tenantName
    ) {
        super(username, password, authorities);
        this.tenantId = tenantId;
        this.tenantName = tenantName;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }
}