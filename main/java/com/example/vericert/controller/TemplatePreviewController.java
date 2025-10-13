package com.example.vericert.controller;

import com.example.vericert.service.SystemVarsBuilder;
import com.example.vericert.service.TemplateService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplatePreviewController {
    private final TemplateService templateService;
    private final SystemVarsBuilder sysVars; // tua classe che calcola serial, verifyUrl, qrBase64, ecc.

    public TemplatePreviewController(TemplateService templateService, SystemVarsBuilder sysVars) {
        this.templateService = templateService;
        this.sysVars = sysVars;
    }

    @PostMapping(value="/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewHtml(@PathVariable Long id, @RequestBody Map<String,Object> userVars) {
        Map<String,Object> sys = sysVars.buildPreviewVars(); // versioni “fake” ma realistiche
        String html = templateService.renderHtml(id, userVars, sys);
        return ResponseEntity.ok(html);
    }
}