package com.example.vericert.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badReq(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> denied(org.springframework.security.access.AccessDeniedException ex,
                                    jakarta.servlet.http.HttpServletRequest request) {

        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        boolean wantsHtml = accept != null && accept.contains("text/html");

        boolean isFormCheckout =
                wantsHtml && (
                        "/api/payments/stripe/checkout-redirect".equals(uri)
                );

        // 🔁 Caso speciale: form HTML che posta su Stripe/PayPal
        // → vogliamo mandare l'utente alla pagina 403 HTML
        if (isFormCheckout) {
            return ResponseEntity
                    .status(HttpStatus.FOUND) // 302
                    .header(org.springframework.http.HttpHeaders.LOCATION, "/403")
                    .build();
        }
        // ✨ Default: tutte le altre API REST rispondono JSON 403
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Accesso negato"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> generic(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Errore interno"));
    }
}