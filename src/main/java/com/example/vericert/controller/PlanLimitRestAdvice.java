package com.example.vericert.controller;

import com.example.vericert.enumerazioni.PlanViolationType;
import com.example.vericert.exception.PlanLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class PlanLimitRestAdvice {

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handlePlanLimit(PlanLimitExceededException ex) {

        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS; // 429 di default

        if (ex.getType() == PlanViolationType.PLAN_EXPIRED) {
            status = HttpStatus.PAYMENT_REQUIRED; // 402
        }

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "error", status.getReasonPhrase(),
                "status", status.value(),
                "message", ex.getMessage(),
                "tenantId", ex.getTenantId(),
                "violation", ex.getType().name()
        );

        return ResponseEntity.status(status).body(body);
    }
}