package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy() {
        return "public/privacy";
    }

    @GetMapping("/cookie-policy")
    public String cookiePolicy() {
        return "public/cookie-policy";
    }
    @GetMapping("/docs")
    public String docs() {
        return "public/docs";
    }
}

