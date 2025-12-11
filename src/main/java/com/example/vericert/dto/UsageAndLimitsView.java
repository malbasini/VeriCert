package com.example.vericert.dto;

import java.math.BigDecimal;

// Stub per mostrare quote in pagina
public record UsageAndLimitsView(
        String planCode,
        String billingCycle,

        long usedCerts,
        long usedApiCalls,
        BigDecimal usedStorageMb,

        long limitCerts,
        long limitApiCalls,
        long limitStorageMb
) {}
