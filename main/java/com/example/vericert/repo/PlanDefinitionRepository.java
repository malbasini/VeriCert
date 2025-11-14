package com.example.vericert.repo;

import com.example.vericert.domain.PlanDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanDefinitionRepository extends JpaRepository<PlanDefinition, Long> {
    Optional<PlanDefinition> findByCode(String code);
}