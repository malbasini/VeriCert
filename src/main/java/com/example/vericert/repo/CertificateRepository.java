package com.example.vericert.repo;
import com.example.vericert.domain.Certificate;
import com.example.vericert.enumerazioni.Stato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> , JpaSpecificationExecutor<Certificate> {
    Optional<Certificate> findById(Long id);
    // elenco per tenant con ricerca semplice (nome/email/serial/course)
    @Query("""
     select c from Certificate c
     where c.tenant.id = :tenantId
       and (
         :q is null or :q = '' or
         lower(c.ownerName) like lower(concat('%',:q,'%')) or
         lower(c.ownerEmail) like lower(concat('%',:q,'%')) or
         lower(c.serial) like lower(concat('%',:q,'%'))
       )
       and (:status is null or c.status = :status)
     """)
    Page<Certificate> search(
            @Param("tenantId") Long tenantId,
            @Param("q") String q,
            @Param("status") Stato status,
            Pageable pageable);

    Optional<Certificate> findByIdAndTenantId(Long id, Long tenantId);

    long countByTenantId(Long tenantId);
}
