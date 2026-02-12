package com.example.vericert.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.InputStream;
import java.nio.file.*;

@RestController
@RequestMapping("/admin/downloads")
public class AdminDownloadController {

    private final Path storageRoot;

    public AdminDownloadController(@Value("${vercert.storage.root:/data/vericert}") String root) {
        this.storageRoot = Paths.get(root).toAbsolutePath().normalize();
    }

    /**
     * Scarica lo ZIP “starter templates”.
     * Prima prova su filesystem:
     *   {root}/storage/downloads/starter-templates/vericert-starter-templates-v1.0.0.zip
     * Se non esiste -> fallback dentro al JAR:
     *   classpath:/starter-templates/vericert-starter-templates-v1.0.0.zip
     */
    @GetMapping("/starter-templates")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Resource> downloadStarterTemplates() {
        String filename = "vericert-starter-templates-v1.0.0.zip";

        // 1) filesystem (persistente via volume)
        Path fsPath = storageRoot
                .resolve("storage")
                .resolve("downloads")
                .resolve("starter-templates")
                .resolve(filename)
                .normalize();

        // Difesa extra: evita path traversal (anche se qui non hai input utente)
        if (!fsPath.startsWith(storageRoot)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            if (Files.exists(fsPath) && Files.isRegularFile(fsPath)) {
                Resource res = new UrlResource(fsPath.toUri());
                long size = Files.size(fsPath);

                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                contentDispositionAttachment(filename))
                        .contentLength(size)
                        .cacheControl(CacheControl.noCache())
                        .body(res);
            }

            // 2) fallback: dentro al JAR
            Resource cp = loadClasspathZip("/starter-templates/" + filename);
            if (cp == null) {
                // 404 pulito (invece di 500)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Se è in classpath lo carico in memoria per avere content-length
            try (InputStream in = cp.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                contentDispositionAttachment(filename))
                        .contentLength(bytes.length)
                        .cacheControl(CacheControl.noCache())
                        .body(new ByteArrayResource(bytes));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String contentDispositionAttachment(String filename) {
        // Content-Disposition safe
        String safe = StringUtils.replace(filename, "\"", "");
        return "attachment; filename=\"" + safe + "\"";
    }

    private Resource loadClasspathZip(String classpathLocation) {
        // Nota: qui NON uso ResourceLoader per tenere il controller “standalone”
        // Se preferisci, posso riscriverlo con ResourceLoader.
        try {
            var url = getClass().getResource(classpathLocation);
            if (url == null) return null;
            return new UrlResource(url);
        } catch (Exception e) {
            return null;
        }
    }
}
