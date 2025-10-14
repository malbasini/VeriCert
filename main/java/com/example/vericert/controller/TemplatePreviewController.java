package com.example.vericert.controller;

import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.SystemVarsBuilder;
import com.example.vericert.service.TemplateService;
import com.example.vericert.util.PdfUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.Console;
import java.util.Map;

@RestController
@RequestMapping()
public class TemplatePreviewController {
    private final TemplateService templateService;
    private final SystemVarsBuilder sysVars; // tua classe che calcola serial, verifyUrl, qrBase64, ecc.

    public TemplatePreviewController(TemplateService templateService,
                                     SystemVarsBuilder sysVars) {
        this.templateService = templateService;
        this.sysVars = sysVars;
    }

    @PostMapping(value="/api/templates/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewHtml(@PathVariable Long id, @RequestBody Map<String,Object> userVars) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        userVars.put("tenantName", tenantName);
        Map<String,Object> sys =  sysVars.buildPreviewVars(); // serial-demo, code-demo, qrBase64 demo, issuedAt=oggi
        return ResponseEntity.ok(templateService.renderHtml(id, userVars, sys));
    }



    @PostMapping(value="/api/templates/{id}/preview.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id, @RequestBody Map<String,Object> userVars) throws Exception {
        Map<String,Object> sys = sysVars.buildPreviewVars();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        String tenantName = user.getTenantName();
        userVars.put("tenantName", tenantName);
        byte[] pdf = PdfUtil.htmlToPdf(templateService.renderHtml(id, userVars, sys));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .body(pdf);
    }

}