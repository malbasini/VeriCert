package com.example.vericert.controller;

import com.example.vericert.component.TenantStorageLayout;
import org.springframework.beans.factory.annotation.Value;
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

    private final Path base;
    private final TenantStorageLayout layout;

    public AdminGetPdfFile(@Value("${vercert.storage.root:/data/vericert}") String rootDir,
                             TenantStorageLayout layout){
        this.base = Paths.get(rootDir).toAbsolutePath().normalize();
        this.layout = layout;
    }

    @GetMapping(path = "/files/{tenantId}/{file}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable Long tenantId,
                                      @PathVariable String file) throws Exception {

        Path baseDir = layout.tenantDir(base, tenantId).toAbsolutePath().normalize();
        Path p = baseDir.resolve(file + ".pdf");
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