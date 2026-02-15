package com.example.vericert.service;

import com.example.vericert.component.TenantStorageLayout;
import com.example.vericert.dto.StoredFile;
import com.example.vericert.util.HashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileSystemCertificateStorageService implements CertificateStorageService {

    private final Path base;
    private final TenantStorageLayout layout;



    public FileSystemCertificateStorageService(@Value("${vercert.storage.root:/data/vericert}") String rootDir,
                                               TenantStorageLayout layout) {
        this.base = Paths.get(rootDir).toAbsolutePath().normalize();
        this.layout = layout;
    }

    private Path tenantDir(Long tenantId) {
        return layout.tenantDir(base, tenantId).toAbsolutePath().normalize();
    }

    private Path pdfPath(Long tenantId, String serial) {
        return tenantDir(tenantId).resolve(serial + ".pdf");
    }

    private String publicUrl(Long tenantId, String serial) {
        // URL servita dal tuo controller statico es. GET /files/{tenantId}/{serial}.pdf
        return "/files/" + tenantId + "/" + serial + ".pdf";
    }

    @Override
    public StoredFile savePdf(Long tenantId, String serial, byte[] pdfBytes) throws IOException {
        Path dir = tenantDir(tenantId);
        Files.createDirectories(dir);

        Path tmp = Files.createTempFile(dir, serial + "-", ".tmp");
        try {
            Files.write(tmp, pdfBytes, StandardOpenOption.TRUNCATE_EXISTING);
            long size = Files.size(tmp);

            // calcolo hash sui bytes già in memoria (ok per PDF tipici).
            // Se i PDF sono grandi, preferisci ricalcolarlo da file in streaming.
            String sha = HashUtils.base64UrlSha256(pdfBytes);

            Path target = pdfPath(tenantId, serial);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return new StoredFile(publicUrl(tenantId, serial), target, size, sha);
        } catch (IOException ex) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] loadPdfBytes(Long tenantId, String serial) throws IOException {
        Path path = pdfPath(tenantId, serial);
        return Files.readAllBytes(path);
    }

    @Override
    public byte[] loadPdfBytesByUrl(String pdfUrl) throws IOException {
        // assume formato /files/{tenantId}/{serial}.pdf
        // parse semplice:
        String[] parts = pdfUrl.split("/");
        if (parts.length < 4) throw new IOException("pdfUrl non valido: " + pdfUrl);
        Long tenantId = Long.valueOf(parts[2]);
        String file = parts[3]; // serial.pdf
        String serial = file.replace(".pdf", "");
        return loadPdfBytes(tenantId, serial);
    }

    @Override
    public boolean exists(Long tenantId, String serial) {
        return Files.exists(pdfPath(tenantId, serial));
    }

    @Override
    public void delete(Long tenantId, String serial) throws IOException {
        Files.deleteIfExists(pdfPath(tenantId, serial));
    }
}
