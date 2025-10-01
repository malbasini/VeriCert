package com.example.vericert.tenancy;

public class TenantContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    public static void set(Long tenantId) { CURRENT.set(tenantId); }
    public static Long get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}