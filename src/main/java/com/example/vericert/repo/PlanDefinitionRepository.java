package com.example.vericert.repo;

import com.example.vericert.domain.PlanDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanDefinitionRepository extends JpaRepository<PlanDefinition, Long> {
    Optional<PlanDefinition> findByCode(String code);


    @Query("""
       select p from PlanDefinition p
       order by p.id
    """)
    List<PlanDefinition> findAllPlains();


}