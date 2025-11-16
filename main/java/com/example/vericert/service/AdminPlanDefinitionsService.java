package com.example.vericert.service;

import com.example.vericert.domain.PlanDefinitions;
import com.example.vericert.repo.PlanDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminPlanDefinitionsService {
    private final PlanDefinitionRepository repo;
    public AdminPlanDefinitionsService(PlanDefinitionRepository repo)
    {
        this.repo = repo;
    }

    public PlanDefinitions getPlan(String code)
    {
        return repo.findByCode(code).orElseThrow();
    }
    public List<PlanDefinitions> getPlans(){
        return repo.findAllPlains();
    }
}
