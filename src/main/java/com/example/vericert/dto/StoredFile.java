package com.example.vericert.dto;

import java.nio.file.Path;

public class StoredFile {
    private final String url;          // es. /files/{tenantId}/{serial}.pdf (servita dal tuo controller)
    private final Path absolutePath;   // es. /opt/vericert/storage/12/ABCD1234.pdf
    private final long sizeBytes;
    private final String sha256Base64Url;

    public StoredFile(String url, Path absolutePath, long sizeBytes, String sha) {
        this.url = url;
        this.absolutePath = absolutePath;
        this.sizeBytes = sizeBytes;
        this.sha256Base64Url = sha;
    }

    public String getUrl() {
        return url;
    }

    public Path getAbsolutePath() {
        return absolutePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256Base64Url() {
        return sha256Base64Url;
    }
}
