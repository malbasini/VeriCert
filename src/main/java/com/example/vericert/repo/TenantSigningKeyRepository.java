package com.example.vericert.repo;

import com.example.vericert.domain.SigningKeyEntity;
import com.example.vericert.domain.TenantSigningKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantSigningKeyRepository extends JpaRepository<TenantSigningKeyEntity, Long> {

    Optional<TenantSigningKeyEntity> findByTenantIdAndStatus(Long tenantId, String status);

    @Query("""
        select sk from TenantSigningKeyEntity tsk
        join SigningKeyEntity sk on sk.kid = tsk.kid
        where tsk.tenantId = :tenantId and tsk.status = 'ACTIVE' and sk.status = 'ACTIVE'
    """)
    Optional<SigningKeyEntity> findActiveSigningKeyByTenant(@Param("tenantId") Long tenantId);

    TenantSigningKeyEntity findByTenantId(Long tenantId);
}