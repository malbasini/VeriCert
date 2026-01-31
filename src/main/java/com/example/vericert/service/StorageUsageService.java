package com.example.vericert.service;

import com.example.vericert.config.VericertStorageProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class StorageUsageService {

    private final VericertStorageProperties storageProperties;

    public StorageUsageService(VericertStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }
    /**
     * Converte i byte in MB con una precisione maggiore.
     * Usiamo 1024 * 1024 per i MiB (standard binario) o 1.000.000 per MB (standard decimale).
     */
    public BigDecimal bytesToMb(long bytes) {
        if (bytes <= 0) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        // Usiamo una scala più alta (es. 4) per i calcoli intermedi e poi arrotondiamo a 2
        BigDecimal b = BigDecimal.valueOf(bytes);
        BigDecimal oneMB = BigDecimal.valueOf(1_000_000); // MB decimale
        return b.divide(oneMB, 2, RoundingMode.HALF_UP);  // 2 decimali
    }
}
