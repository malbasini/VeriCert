package com.example.vericert.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.*;

@RestControllerAdvice(assignableTypes = {}) // globale
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    /** Gestiamo solo le richieste /api/** per JSON */
    private boolean isApi(WebRequest request) {
        String path = Optional.ofNullable(request.getDescription(false))
                .orElse(""); // e.g. uri=/api/...
        return path.contains("uri=/api/");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException ex, WebRequest req){
        if (!isApi(req)) return null; // lascia ad altri handler (MVC)
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Risorsa non trovata");
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create("about:blank"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, WebRequest req){
        if (!isApi(req)) return null;
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Operazione non consentita");
        pd.setDetail("Non hai i permessi necessari.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, WebRequest req){
        if (!isApi(req)) return null;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("error", "Validation failed");
        List<Map<String,String>> errors = new ArrayList<>();
        for (FieldError fe: ex.getBindingResult().getFieldErrors()){
            errors.add(Map.of(
                    "field", fe.getField(),
                    "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid")
            ));
        }
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex, WebRequest req){
        if (!isApi(req)) return null;
        List<Map<String,String>> errors = ex.getConstraintViolations().stream()
                .map(v -> Map.of(
                        "property", property(v),
                        "message", v.getMessage()
                )).toList();
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "error", "Constraint violation", "errors", errors
        ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<?> handleMethodValidation(HandlerMethodValidationException ex, WebRequest req){
        if (!isApi(req)) return null;
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "error", "Method validation failed", "detail", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, WebRequest req){
        if (!isApi(req)) return null;
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Errore interno");
        pd.setDetail("Si è verificato un errore inatteso. Riprovare.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private static String property(ConstraintViolation<?> v){
        String p = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
        return p;
    }
}
