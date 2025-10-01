package com.example.vericert.repo;

import com.example.vericert.domain.UsageMeter;
import com.example.vericert.domain.UsageMeterId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsageMeterRepository extends JpaRepository<UsageMeter, UsageMeterId> {
    Optional<UsageMeter> findByIdTenantIdAndIdYm(Long tenantId, String ym);
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO usage_meter (tenant_id, ym, cert_count, api_calls, storage_bytes)
    VALUES (:tenantId, :ym, :delta, 0, 0)
    ON DUPLICATE KEY UPDATE cert_count = cert_count + :delta
    """, nativeQuery = true)
    void upsertCertCount(@Param("tenantId") Long tenantId, @Param("ym") String ym, @Param("delta") int delta);
}