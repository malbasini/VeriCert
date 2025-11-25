package com.example.vericert.service;

import com.example.vericert.component.PaypalClient;
import com.example.vericert.dto.PaypalSubscriptionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class PaypalSubscriptionService {

    private final PaypalClient paypalClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaypalSubscriptionService(PaypalClient paypalClient) {
        this.paypalClient = paypalClient;
    }

    /**
     * Recupera i dettagli di una subscription PayPal e li mappa in un DTO semplificato.
     */
    public PaypalSubscriptionDto findById(String subscriptionId) {
        // Risposta generica come mappa
        Map<String, Object> raw = paypalClient.get("/v1/billing/subscriptions/" + subscriptionId, Map.class);

        String status = (String) raw.get("status");
        String planId = (String) raw.get("plan_id");

        @SuppressWarnings("unchecked")
        Map<String, Object> customFields = (Map<String, Object>) raw.get("custom_id");
        // Se usi custom_id come stringa JSON con i tuoi riferimenti, puoi gestirla così:
        String tenantId = null;
        String planCode = null;
        String billingCycle = null;

        Object customIdObj = raw.get("custom_id");
        if (customIdObj instanceof String customIdStr && !customIdStr.isEmpty()) {
            try {
                // es: custom_id = {"tenant_id":"1","plan_code":"BASIC","billing_cycle":"MONTHLY"}
                Map<String, String> custom = objectMapper.readValue(customIdStr, Map.class);
                tenantId = custom.get("tenant_id");
                planCode = custom.get("plan_code");
                billingCycle = custom.get("billing_cycle");
            } catch (Exception ignored) {
            }
        }

        // current_period_* di solito stanno in "billing_info"
        @SuppressWarnings("unchecked")
        Map<String, Object> billingInfo = (Map<String, Object>) raw.get("billing_info");

        Instant start = null;
        Instant end = null;
        if (billingInfo != null) {
            Object lastPaymentTime = billingInfo.get("last_payment_time");
            Object nextBillingTime = billingInfo.get("next_billing_time");
            // questi campi spesso sono stringhe ISO 8601
            if (lastPaymentTime instanceof String s1) {
                start = Instant.parse(s1);
            }
            if (nextBillingTime instanceof String s2) {
                end = Instant.parse(s2);
            }
        }

        return new PaypalSubscriptionDto(
                subscriptionId,
                status,
                planId,
                tenantId,
                planCode,
                billingCycle,
                start,
                end
        );
    }
}
