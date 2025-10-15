package com.example.vericert.controller;

import com.example.vericert.dto.PreviewDto;
import com.example.vericert.service.CustomUserDetails;
import com.example.vericert.service.SystemVarsBuilder;
import com.example.vericert.service.TemplateService;
import com.example.vericert.util.PdfUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @PostMapping(
            value="/api/templates/{id}/{tenant}/preview",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<?> previewHtml(@PathVariable Long id,
                                         @PathVariable String tenant,
                                         @Valid @RequestBody PreviewDto dto,
                                         BindingResult br) {
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message","Validation failed","errors",errors));
        }
        Map<String,Object> map = toMap(dto);
        map.put("tenantName", tenant);
        Map<String,Object> sys = sysVars.buildPreviewVars();
        String html = templateService.renderHtml(id, map, sys);
        return ResponseEntity.ok(html);
    }


    public static Map<String, Object> toMap(Object record) {
        try {
            var c = record.getClass();
            if (!c.isRecord()) throw new IllegalArgumentException("Not a record");
            var map = new java.util.HashMap<String,Object>();
            for (var comp : c.getRecordComponents()) {
                var accessor = comp.getAccessor();
                map.put(comp.getName(), accessor.invoke(record));
            }
            return map;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value="/api/templates/{id}/{tenant}/preview.pdf",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id,
                                             @PathVariable String tenant,
                                             @Valid @RequestBody PreviewDto dto,
                                             BindingResult br) throws Exception {

        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            // Per coerenza, ritorna JSON anche qui (il client lo intercetta e mostra)
            var json = new ObjectMapper().writeValueAsBytes(Map.of("message","Validation failed","errors",errors));
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        }
        Map<String,Object> map = toMap(dto);
        map.put("tenantName", tenant);
        Map<String,Object> sys = sysVars.buildPreviewVars();
        byte[] pdf = PdfUtil.htmlToPdf(templateService.renderHtml(id, map, sys));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .body(pdf);
    }

}