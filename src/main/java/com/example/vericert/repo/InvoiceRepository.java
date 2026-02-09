package com.example.vericert.repo;

import com.example.vericert.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Invoice> findByPublicCode(String publicCode);

    List<Invoice> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    
    @Query("""
        SELECT COALESCE(MAX(i.numberSeq), 0)
        FROM Invoice i
        WHERE i.tenantId = :tenantId AND i.issueYear = :year
    """)
    Long maxSeqForYear(@Param("tenantId") Long tenantId, @Param("year") Integer year);

    Optional<Invoice> findByInvoiceCode(String invoiceCode);


    @Query("""
        SELECT i
        FROM Invoice i
        WHERE i.tenantId = :tenantId AND i.invoiceSave = true
    """)
    
    Optional<Invoice> findByTenantIdAndInvoiceSave(@Param("tenantId") Long tenantId);

    Optional<Invoice> findByTenantIdAndPublicCode(Long tenantId, String publicCode);


    @Query("""
     select c from Invoice c
     where c.tenantId = :tenantId
       and (
         :q is null or :q = '' or
         lower(c.customerName) like lower(concat('%',:q,'%')) or
         lower(c.customerEmail) like lower(concat('%',:q,'%')) or
         lower(c.publicCode) like lower(concat('%',:q,'%'))
       )
     """)
    Page<Invoice> search(
            @Param("tenantId") Long tenantId,
            @Param("q") String q,
            Pageable pageable);




}
