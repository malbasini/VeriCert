package com.example.vericert.component;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Profile("prod")
@Component
public class ProdTenantStorageLayout implements TenantStorageLayout {
    @Override
    public Path tenantDir(Path base, long tenantId) {
        return base.resolve("storage").resolve(String.valueOf(tenantId));
    }
}