package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vui")
public class VerificationUiController {

    @GetMapping("/{code}")
    public String verificationUi(Model model, @PathVariable String code) {
        model.addAttribute("code", code);
        // Non serve nemmeno il codice nel model, lo leggiamo da JS con window.location
        return "verification/verifica-certificati"; // templates/public/verification-ui.html
    }
}
