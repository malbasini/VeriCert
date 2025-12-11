package com.example.vericert.service;

import com.example.vericert.dto.StoredFile;

import java.io.IOException;

public interface CertificateStorageService {
    StoredFile savePdf(Long tenantId, String serial, byte[] pdfBytes) throws IOException;
    byte[] loadPdfBytes(Long tenantId, String serial) throws IOException;

    // Helper comodi quando parti dalla riga DB:
    byte[] loadPdfBytesByUrl(String pdfUrl) throws IOException;   // se salvi una url/relative path
    boolean exists(Long tenantId, String serial);
    void delete(Long tenantId, String serial) throws IOException;
}
