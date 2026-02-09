package com.example.vericert.controller;

import com.example.vericert.exception.PlanLimitExceededException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class PlanLimitMvcAdvice {

    @ExceptionHandler(PlanLimitExceededException.class)
    public String handlePlanLimit(PlanLimitExceededException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("violationType", ex.getType().name());
        model.addAttribute("tenantId", ex.getTenantId());
        // view tipo src/main/resources/templates/error/plan-limit.html
        return "error/plan-limit";
    }
}