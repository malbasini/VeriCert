package com.example.vericert.repo;

import com.example.vericert.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Long findByTenantId(Long tenantId);

    // Se ti serve solo l'id, proiezione semplice
    interface TemplateIdOnly {
        Long getId();
    }
    Optional<TemplateIdOnly> findFirstProjectedByTenant_Id(Long tenantId);
}
