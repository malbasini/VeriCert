package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SignupViewController {

    @GetMapping("/signup")
    public String signupForm() {
        return "security/signup"; // cerca signup.html in src/main/resources/template
    }
}