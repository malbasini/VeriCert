package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vui")
public class VerificationUiController {

    @GetMapping("/{code}")
    public String verificationUi() {
        // Non serve nemmeno il codice nel model, lo leggiamo da JS con window.location
        return "verification/result"; // templates/public/verification-ui.html
    }
}
