package com.example.vericert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileUploadService {

    @Value("${vericert.base-url}")
    private String baseUrl;

    private static final String STORAGE_BASE_DIR = "/opt/vericert";

    /**
     * Salva un file (logo o signature) nella cartella del tenant
     * @param tenantId ID del tenant
     * @param file Il file da uploadare
     * @param fileName Nome del file (es: "logo.png" o "signature.png")
     * @return URL pubblico del file salvato
     */
    public String saveFile(Long tenantId, MultipartFile file, String fileName) throws IOException {
        // Valida che il file non sia vuoto
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Il file è vuoto");
        }

        // Valida il tipo di file (solo immagini)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Il file deve essere un'immagine");
        }

        // Crea la directory del tenant se non esiste
        Path tenantDir = Paths.get(STORAGE_BASE_DIR, tenantId.toString());
        if (!Files.exists(tenantDir)) {
            Files.createDirectories(tenantDir);
        }

        // Determina l'estensione dal content type o dal nome file
        String extension = getFileExtension(file);
        String baseFileName = fileName.contains(".")
            ? fileName.substring(0, fileName.lastIndexOf('.'))
            : fileName;
        String finalFileName = baseFileName + extension;

        // Salva il file
        Path targetPath = tenantDir.resolve(finalFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Restituisce l'URL pubblico
        return baseUrl + "/storage/" + tenantId + "/" + finalFileName;
    }

    /**
     * Elimina un file dalla cartella del tenant
     */
    public void deleteFile(Long tenantId, String fileName) throws IOException {
        Path filePath = Paths.get(STORAGE_BASE_DIR, tenantId.toString(), fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * Estrae l'estensione dal file
     */
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // Fallback basato su content type
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.equals("image/png")) return ".png";
            if (contentType.equals("image/jpeg")) return ".jpg";
            if (contentType.equals("image/gif")) return ".gif";
            if (contentType.equals("image/webp")) return ".webp";
        }

        return ".png"; // default
    }
}
