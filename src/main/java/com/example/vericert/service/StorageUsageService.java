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
        if (bytes <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal mb = BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP); // calcolo preciso
        int scale = (mb.compareTo(BigDecimal.ONE) >= 0) ? 2 : 3; // sotto 1MB: 3 decimali
        return mb.setScale(scale, RoundingMode.HALF_UP);
    }
}
