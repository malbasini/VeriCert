package com.example.vericert.service;

import com.example.vericert.domain.Tenant;
import com.example.vericert.repo.TenantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    public long currentTenantId()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        return user.getTenantId();
    }

    public Tenant ref(Long tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow();
    }
}
