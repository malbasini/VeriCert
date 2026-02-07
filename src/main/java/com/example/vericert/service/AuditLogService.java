package com.example.vericert.service;

import com.example.vericert.domain.AuditLog;
import com.example.vericert.repo.AuditLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo) { this.repo = repo; }
    @Transactional
    public void log(Long tenantId, String actor, String event, String details) {
        var a = new AuditLog(); a.setTenantId(tenantId); a.setActor(actor); a.setEntity(event); a.setPayload(details);
        repo.save(a);
    }
}