package com.example.vericert.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class MvcExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException ex, Model model){
        model.addAttribute("status", 404);
        model.addAttribute("title", "Risorsa non trovata");
        model.addAttribute("message", ex.getMessage());
        return "error"; // template/error.html
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String generic(Exception ex, Model model){
        model.addAttribute("status", 500);
        model.addAttribute("title", "Errore interno");
        model.addAttribute("message", "Si è verificato un errore inatteso. Riprovare.");
        // opzionale: loggare ex
        return "error/error";
    }
}