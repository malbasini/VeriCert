package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPagesController {

    @GetMapping("/403")
    public String forbidden(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("title", "Accesso negato");
        model.addAttribute("message", "Non hai i permessi per accedere a questa operazione.");
        return "error/403"; // templates/error/403.html
    }
}