package com.example.vericert.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final Long tenantId;
    private final String tenantName;
    private final String username;
    private final String password;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(
            Long userId,
            Long tenantId,
            String tenantName,
            String username,
            String password,
            String email,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled
    ) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.username = username;
        this.password = password;
        this.email = email;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public Long getUserId()     { return userId; }
    public Long getTenantId()   { return tenantId; }
    public String getTenantName(){ return tenantName; }
    public String getEmail(){ return email; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities(){ return authorities; }
    @Override public String getPassword(){ return password; }
    @Override public String getUsername(){ return username; }
    @Override public boolean isAccountNonExpired(){ return true; }
    @Override public boolean isAccountNonLocked(){ return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled(){ return enabled; }
}
