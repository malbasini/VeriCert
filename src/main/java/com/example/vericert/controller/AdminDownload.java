package com.example.vericert.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class AdminDownload {

    @Value("${vercert.storage.root:/data/vericert}")
    private String storageRoot;

    @GetMapping("/admin/downloads/starter-templates")
    public void downloadStarterTemplates(HttpServletResponse response) throws IOException {
        Path zip = Paths.get(storageRoot, "downloads", "starter-templates",
                "vericert-starter-templates-v1.0.0.zip");

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"vericert-starter-templates-v1.0.0.zip\"");
        response.setHeader("Content-Length", String.valueOf(Files.size(zip)));

        try (var in = Files.newInputStream(zip);
             var out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }




}
