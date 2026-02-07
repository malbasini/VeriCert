package com.example.vericert.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class AdminGetPdfFile {
    @GetMapping(path = "/files/{tenantId}/{file}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable Long tenantId,
                                      @PathVariable String file) throws Exception {
        Path p = null;
        try {
            p = Paths.get("storage/", tenantId.toString(), file + ".pdf");
        }
        catch (Exception e) {
            String c = e.getMessage();
            System.out.println(c);
        }
        assert p != null;
        if (!Files.exists(p)) return ResponseEntity.notFound().build();
        byte[] bytes = Files.readAllBytes(p);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"" + file + ".pdf\"")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Accept-Ranges", "bytes")
                .contentLength(bytes.length)
                .body(bytes);
    }
}