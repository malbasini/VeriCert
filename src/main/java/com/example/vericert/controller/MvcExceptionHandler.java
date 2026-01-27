package com.example.vericert.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class MvcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MvcExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException ex, Model model){
        model.addAttribute("status", 404);
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("title", "Risorsa non trovata");
        model.addAttribute("description", "Risorsa non trovata.");
        return "error/error";
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied(Exception ex, Model model){
        model.addAttribute("status", 403);
        model.addAttribute("message", "Non hai accesso alla risorsa");
        model.addAttribute("title", "Accesso negato");
        model.addAttribute("description", "Accesso negato");
        return "error/403";
    }


        @ExceptionHandler(Exception.class)
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public String generic(Exception ex, Model model, HttpServletRequest request){
            String uri = request.getRequestURI();
            if ("/favicon.ico".equals(uri)) {
                // opzionale: non loggare come errore
                return "forward:/"; // o una 404 custom
            }
            log.error("Errore non gestito su {} {}", request.getMethod(), uri, ex);
            model.addAttribute("status", 500);
            model.addAttribute("title", "Errore interno");
            model.addAttribute("message", "Si è verificato un errore inatteso. Riprovare.");
            return "error/error";
        }
    }
