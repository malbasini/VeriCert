package com.example.vericert.service;

import com.example.vericert.config.VericertStorageProperties;
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
    public BigDecimal calculateCurrentStorageMb(Long tenantId) {
        Path tenantRoot = Paths.get(storageProperties.getLocalPath(), tenantId.toString());

        if (!Files.exists(tenantRoot) || !Files.isDirectory(tenantRoot)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalBytes = 0L;

        // camminiamo ricorsivamente nella cartella del tenant
        try (Stream<Path> walk = Files.walk(tenantRoot)) {
            totalBytes = walk
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            // se un file dà errore di lettura, lo consideriamo 0 e andiamo avanti
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            // se proprio non riusciamo a camminare la cartella, restituiamo 0
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Conversione byte -> MB decimali (1 MB = 1_000_000 byte)
        BigDecimal mb = new BigDecimal(totalBytes)
                .divide(new BigDecimal(1_000_000), 2, RoundingMode.HALF_UP);

        return mb;
    }
}
