package com.example.vericert.component;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Profile("dev")
@Component
public class DevTenantStorageLayout implements TenantStorageLayout {
    @Override
    public Path tenantDir(Path base, long tenantId) {
        return base.resolve(String.valueOf(tenantId));
    }
}