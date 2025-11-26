package com.example.vericert.dto;

import java.time.Instant;

public record PaypalSubscriptionDto(
        String id,
        String status,
        String planId,
        String tenantId,
        String planCode,
        String billingCycle,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        String amountValue,
        String amountCurrency
) {}
