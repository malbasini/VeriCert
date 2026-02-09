package com.example.vericert.service;

import com.example.vericert.domain.Payment;

import java.util.Map;

public interface PaymentGateway {
    Payment createPayment(Long tenantId, long amountMinor, String currency, String description,
                          Map<String, String> metadata, String idempotencyKey) throws Exception;

    void handleWebhook(String payload, String signatureHeader) throws Exception;
}
