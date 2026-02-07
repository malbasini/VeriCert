package com.example.vericert.repo;

import com.example.vericert.domain.Template;
import com.example.vericert.domain.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Page<Template> searchByNameContainingIgnoreCase(String q, Pageable pageable);

    Template findByName(String tenantName);

    long countByTenantId(Long tenantId);

    Optional<Template> findByTenant(Tenant locked);

    // Se ti serve solo l'id, proiezione semplice
    interface TemplateIdOnly {
        Long getId();
    }
    Optional<TemplateIdOnly> findFirstProjectedByTenant_Id(Long tenantId);
    Optional<Template> findByTenantIdAndId(Long tenantId, Long id);
    boolean existsByTenantIdAndNameAndVersion(Long tenantId, String name, String version);

    @Modifying
    @Query("update Template t set t.active=false where t.tenant.id=:tenantId")
    void deactivateAll(@Param("tenantId") Long tenantId);

    @Query("""
           select t
           from Template t
           where t.tenant.id = :tenantId
           order by t.updatedAt desc
           """)
    Page<Template> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("""
           select t
           from Template t
           where t.tenant.id = :tenantId
             and lower(t.name) like lower(concat('%', :q, '%'))
           order by t.updatedAt desc
           """)
    Page<Template> searchByName(@Param("tenantId") Long tenantId,
                                @Param("q") String q,
                                Pageable pageable);


    @Modifying
    @Query("delete from Template t where t.id = :id and t.tenant.id = :tenantId")
    int deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    Optional<Template> findByTenantIdAndActiveTrue(Long tenantId);
    // opzionale se vuoi una fallback: il più recente tra gli attivi
    Optional<Template> findFirstByTenantIdAndActiveTrueOrderByUpdatedAtDesc(Long tenantId);




    @Query("""
           select t
           from Template t
           where t.tenant.id = 0
           order by t.updatedAt desc
           """)
    List<Template> findAllByTenantZero();





}

