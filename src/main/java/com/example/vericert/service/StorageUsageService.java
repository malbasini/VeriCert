package com.example.vericert.service;

import com.example.vericert.config.VericertStorageProperties;
import com.example.vericert.domain.UsageMeter;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.util.stream.Stream;

@Service
public class StorageUsageService {

    private final VericertStorageProperties storageProperties;

    public StorageUsageService(VericertStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * Calcola quanti MB di storage sta usando questo tenant,
     * sommando tutti i file nella directory {basePath}/{tenantId}
     *
     * Se la cartella non esiste ancora, ritorna 0.00
     */
    public BigDecimal calculateCurrentStorageMb(Long tenantId, UsageMeter m) {
        Path tenantRoot = Paths.get(storageProperties.getLocalPath(), tenantId.toString());
        if (!Files.exists(tenantRoot) || !Files.isDirectory(tenantRoot)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long totalBytes = m.getPdfStorageMb().multiply(BigDecimal.valueOf(1024 * 1024)).longValue();
        return calculateSizeMb(totalBytes);
    }
    /**
     * Converte i byte in MB con una precisione maggiore.
     * Usiamo 1024 * 1024 per i MiB (standard binario) o 1.000.000 per MB (standard decimale).
     */
    public BigDecimal bytesToMb(long bytes) {
        if (bytes <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Usiamo una scala più alta (es. 4) per i calcoli intermedi e poi arrotondiamo a 2
        return BigDecimal.valueOf(bytes)
                .divide(BigDecimal.valueOf(1024 * 1024), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Invece di scansionare tutto il disco, calcola i MB di un singolo incremento.
     */
    public BigDecimal calculateSizeMb(long fileSizeBytes) {
        return bytesToMb(fileSizeBytes);
    }
}
