package com.example.vericert.repo;

import com.example.vericert.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    @Query("select t.id from Tenant t where t.slug = :slug")
    Optional<Long> findIdBySlug(@Param("slug") String slug);
}
