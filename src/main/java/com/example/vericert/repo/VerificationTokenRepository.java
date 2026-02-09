package com.example.vericert.repo;

import com.example.vericert.domain.VerificationToken;
import com.example.vericert.dto.VerificationView;
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

    Optional<VerificationToken> findByCode(String code);

    Optional<VerificationToken> findByJti(String jti);


    @Query("""
      SELECT new com.example.vericert.dto.VerificationView(
          t.code, t.jti, t.kid, t.expiresAt,
          t.certificateId, c.serial, c.ownerName, c.issuedAt, c.pdfUrl, t.compactJws, t.sha256Cached, c.tenantId)
      FROM VerificationToken t
      JOIN Certificate c ON c.id = t.certificateId
      WHERE t.code = :code
    """)
    Optional<VerificationView> findViewByCode(@Param("code") String code);

    void deleteByCertificateId(Long id);

    VerificationToken findByCertificateId(Long id);
}