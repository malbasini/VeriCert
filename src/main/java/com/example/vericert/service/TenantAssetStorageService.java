package com.example.vericert.service;

import com.example.vericert.component.TenantStorageLayout;
import com.example.vericert.config.VericertProps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class TenantAssetStorageService {

    private final Path base;
    private final VericertProps props;
    private final TenantStorageLayout layout;


    public TenantAssetStorageService(@Value("${vercert.storage.root:/data/vericert}") String rootDir,
                                     VericertProps props,
                                     TenantStorageLayout layout) {
        this.base = Paths.get(rootDir).toAbsolutePath().normalize();
        this.props = props;
        this.layout = layout;
    }

    public Path signaturePath(long tenantId) throws IOException {

        Path baseDir = layout.tenantDir(base, tenantId).toAbsolutePath().normalize();
        Path p = baseDir.resolve("signature.png");
        Files.createDirectories(p.getParent());
        return p;
    }

    public Path logoPath(long tenantId) throws IOException {
        Path baseDir = layout.tenantDir(base, tenantId).toAbsolutePath().normalize();
        Path p = baseDir.resolve("logo.png");
        Files.createDirectories(p.getParent());
        return p;
    }

    public void saveSignature(long tenantId, MultipartFile file) throws IOException {
        Files.copy(file.getInputStream(), signaturePath(tenantId), StandardCopyOption.REPLACE_EXISTING);
    }

    public void saveLogo(long tenantId, MultipartFile file) throws IOException {
        Files.copy(file.getInputStream(), logoPath(tenantId), StandardCopyOption.REPLACE_EXISTING);
    }

    public String signaturePublicUrl(long tenantId) {
        return props.getBaseUrl() + "/storage/" + tenantId + "/signature.png";
    }

    public String logoPublicUrl(long tenantId) {
        return props.getBaseUrl() + "/storage/" + tenantId + "/logo.png";
    }

}


