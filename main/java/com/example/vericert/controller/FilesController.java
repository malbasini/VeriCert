package com.example.vericert.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.*;

@RestController
public class FilesController {

    @Value("${vericert.storage.local-path}")
    private String storagePath;

    @GetMapping(value = "/files/certificates/{fileName:.+}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> getPdf(@PathVariable String fileName) throws Exception {
        Path base = Paths.get(storagePath).toAbsolutePath().normalize();
        Path file = base.resolve("certificates").resolve(fileName).normalize();
        if (!file.startsWith(base) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource res = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(res);
    }
}
