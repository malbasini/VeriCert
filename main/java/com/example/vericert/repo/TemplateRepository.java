package com.example.vericert.repo;

import com.example.vericert.domain.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    // Se ti serve solo l'id, proiezione semplice
    interface TemplateIdOnly {
        Long getId();
    }
    Optional<TemplateIdOnly> findFirstProjectedByTenant_Id(Long tenantId);
    Page<Template> findByTenantId(Long tenantId, Pageable pageable);
    Optional<Template> findByTenantIdAndId(Long tenantId, Long id);
    boolean existsByTenantIdAndNameAndVersion(Long tenantId, String name, String version);

    @Modifying
    @Query("update Template t set t.active=false where t.tenant.id=:tenantId")
    void deactivateAll(@Param("tenantId") Long tenantId);

}
