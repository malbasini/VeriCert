package com.example.vericert.controller;

import com.example.vericert.dto.PreviewDto;
import com.example.vericert.service.SystemVarsBuilder;
import com.example.vericert.service.TemplateService;
import com.example.vericert.util.PdfUtil;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

import static com.example.vericert.util.MapUtils.toMap;

@RestController
@RequestMapping()
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class TemplatePreviewController {
    private final TemplateService templateService;
    private final SystemVarsBuilder sysVars; // tua classe che calcola serial, verifyUrl, qrBase64, ecc.
    public TemplatePreviewController(TemplateService templateService,
                                     SystemVarsBuilder sysVars) {
        this.templateService = templateService;
        this.sysVars = sysVars;
    }
    @PostMapping(value="/api/templates/{id}/{tenant}/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> previewHtml(@PathVariable Long id,
                                         @PathVariable String tenant,
                                         @Valid @RequestBody PreviewDto dto,
                                         BindingResult br) {
        try
        {
            int hours = Integer.parseInt(String.valueOf(dto.hours()));
        }
        catch (NumberFormatException e){
            br.addError(new FieldError("hours","hours","Il numero ore deve essere un numero intero"));
        }
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
        try {
            Map<String, Object> map = toMap(dto);
            map.put("tenantName", tenant);
            Map<String, Object> sys = sysVars.buildPreviewVars();
            String html = templateService.renderHtml(id, map, sys);
            return ResponseEntity.ok(html);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
    }
    @PostMapping(value="/api/templates/{id}/{tenant}/preview.pdf", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<? extends Object> previewPdf(@PathVariable Long id,
                                                          @PathVariable String tenant,
                                                          @Valid @RequestBody PreviewDto dto,
                                                          BindingResult br) throws Exception {

        try
        {
            int hours = Integer.parseInt(String.valueOf(dto.hours()));
        }
        catch (NumberFormatException e){
            br.addError(new FieldError("hours","hours","Il numero ore deve essere un numero intero"));
        }
        if (br.hasErrors()) {
            var errors = br.getFieldErrors().stream()
                    .collect(Collectors.groupingBy(
                            FieldError::getField,
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));
            // Per coerenza, ritorna JSON anche qui (il client lo intercetta e mostra)
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message","Validation failed","errors",errors));
        }
        Map<String,Object> map = toMap(dto);
        map.put("tenantName", tenant);
        byte[] pdf = null;
        try {
        Map<String,Object> sys = sysVars.buildPreviewVars();
            pdf = PdfUtil.htmlToPdf(templateService.renderHtml(id, map, sys));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .body(pdf);
    }

}