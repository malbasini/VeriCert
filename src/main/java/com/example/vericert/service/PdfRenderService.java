package com.example.vericert.service;

import org.springframework.stereotype.Service;

@Service
public class PdfRenderService {
    public byte[] render(String html) {
        try (var out = new java.io.ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("PDF render failed: " + e.getMessage(), e);
        }
    }
}
