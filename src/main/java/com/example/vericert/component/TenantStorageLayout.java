package com.example.vericert.component;

import java.nio.file.Path;

public interface TenantStorageLayout {
    Path tenantDir(Path base, long tenantId);
}