package com.example.vericert.util;

import java.io.ByteArrayOutputStream;

public class PdfUtil {
    public static byte[] htmlToPdf(String html){
        try (var out = new ByteArrayOutputStream()){
            var builder = new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.withHtmlContent(html, null); builder.toStream(out); builder.run();
            return out.toByteArray();
        } catch(Exception e){ throw new RuntimeException(e); }
    }
}