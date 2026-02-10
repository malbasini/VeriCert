package com.example.vericert.service;

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


    public TenantAssetStorageService(@Value("${vercert.storage.root:/data/vericert}") String rootDir,
                                     VericertProps props) {
        this.base = Paths.get(rootDir).toAbsolutePath().normalize();
        this.props = props;
    }

    public Path signaturePath(long tenantId) throws IOException {
        Path p = base.resolve("storage").resolve(String.valueOf(tenantId)).resolve("signature.png")
                .toAbsolutePath().normalize();
        Files.createDirectories(p.getParent());
        return p;
    }

    public Path logoPath(long tenantId) throws IOException {
        Path p = base.resolve("storage").resolve(String.valueOf(tenantId)).resolve("logo.png")
                .toAbsolutePath().normalize();
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
        return props.getBaseUrl() + "/files/" + tenantId + "/signature.png";
    }

    public String logoPublicUrl(long tenantId) {
        return props.getBaseUrl() + "/files/" + tenantId + "/logo.png";
    }

}


