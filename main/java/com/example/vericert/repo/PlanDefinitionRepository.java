package com.example.vericert.repo;

import com.example.vericert.domain.Membership;
import com.example.vericert.domain.PlanDefinitions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanDefinitionRepository extends JpaRepository<PlanDefinitions, Long> {
    Optional<PlanDefinitions> findByCode(String code);


    @Query("""
       select p from PlanDefinitions p
       order by p.id
    """)
    List<PlanDefinitions> findAllPlains();


}