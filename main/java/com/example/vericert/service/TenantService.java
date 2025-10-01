package com.example.vericert.service;

import com.example.vericert.domain.Tenant;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.tenancy.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantService {
    
    private final TenantRepository tenantRepository;
    
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }
    public Tenant currentTenantOrThrow() throws IllegalStateException
    {
        Long idt = TenantContext.get();
        if (idt == null) {
            throw new IllegalStateException("No tenant set in the context");
        }
        return tenantRepository.findById(idt).orElseThrow();
    }
}
