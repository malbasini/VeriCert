package com.example.vericert.repo;

import com.example.vericert.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;



public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // 1) tenant_id dal code (se ti serve in futuro)
    @Query(value = """

            select c.tenant_id
        from verification_token vt
        join certificate c on c.id = vt.certificate_id
        where vt.code = :code
        limit 1
        """, nativeQuery = true)
    Optional<Long> findTenantIdByCode(@Param("code") String code);

    // 2) Vista completa per la pagina pubblica (bypassa filtri)
    @Query(value = """
        select vt.code as code,
               c.serial as serial,
               c.owner_name as ownerName,
               c.course_code as courseCode,
               c.issued_at as issuedAt,
               c.revoked_at as revoked,
               c.revoked_reason as revokedReason,
               c.tenant_id as tenantId
        from verification_token vt
        join certificate c on c.id = vt.certificate_id
        where vt.code = :code
        """, nativeQuery = true)
    Optional<VerificationView> findViewByCode(@Param("code") String code);

    Optional<VerificationToken> findByCode(String code);
}