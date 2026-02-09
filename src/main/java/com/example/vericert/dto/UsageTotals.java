package com.example.vericert.dto;

import java.math.BigDecimal;

public record UsageTotals(
        long certs,
        long apiCalls,
        BigDecimal storageMb
) {}
