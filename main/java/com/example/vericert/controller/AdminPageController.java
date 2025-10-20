package com.example.vericert.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Solo se non ce l’hai già
@Controller
public class AdminPageController {
    @GetMapping("/admin/templates/preview")
    public String templatePreviewPage() {
        return "admin/templates-preview";
    }
}
