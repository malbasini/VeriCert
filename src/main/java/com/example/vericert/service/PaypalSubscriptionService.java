package com.example.vericert.service;

import com.example.vericert.component.PaypalClient;
import com.example.vericert.dto.PaypalSubscriptionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PaypalSubscriptionService {

    private final PaypalClient paypalClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaypalSubscriptionService(PaypalClient paypalClient) {
        this.paypalClient = paypalClient;
    }

    public PaypalSubscriptionDto findById(String subscriptionId) {
        // Leggo la subscription da PayPal
        Map<String, Object> raw = paypalClient.get("/v1/billing/subscriptions/" + subscriptionId, Map.class);
        System.out.println("RAW SUB FROM PAYPAL = " + raw);

        String status = (String) raw.get("status");
        System.out.println("STATUS FROM PAYPAL = " + status);

        String planId = (String) raw.get("plan_id");

        // ---------- custom_id: è una STRINGA ----------
        String tenantId = null;
        String planCode = null;
        String billingCycle = null;

        Object customIdObj = raw.get("custom_id");
        if (customIdObj instanceof String customIdStr && !customIdStr.isBlank()) {
            try {
                // es: custom_id = {"tenant_id":"1","plan_code":"PRO","billing_cycle":"MONTHLY"}
                Map<String, String> custom = objectMapper.readValue(
                        customIdStr,
                        new TypeReference<Map<String, String>>() {}
                );
                tenantId = custom.get("tenant_id");
                planCode = custom.get("plan_code");
                billingCycle = custom.get("billing_cycle");
            } catch (Exception e) {
                System.out.println(">>> Impossibile parsare custom_id: " + customIdStr);
            }
        } else {
            System.out.println(">>> custom_id non è una String: " + customIdObj);
        }

        // ---------- Periodi dal billing_info (se ti servono) ----------
        Instant start = null;
        Instant end = null;

        Object billingInfoObj = raw.get("billing_info");
        if (billingInfoObj instanceof Map<?, ?> billingInfo) {
            // in molte response PayPal hai "last_payment" come oggetto con campo "time"
            Object lastPaymentObj = billingInfo.get("last_payment");
            if (lastPaymentObj instanceof Map<?, ?> lastPayment) {
                Object timeObj = lastPayment.get("time");
                if (timeObj instanceof String ts) {
                    start = Instant.parse(ts);
                }
            }

            // oppure direttamente "next_billing_time" come stringa ISO
            Object nextBillingTimeObj = billingInfo.get("next_billing_time");
            if (nextBillingTimeObj instanceof String ts2) {
                try {
                    end = Instant.parse(ts2);
                } catch (Exception ignored) {}
            }
        }

        // ---------- Importo: lo ricavo dal Plan ----------
        String amountValue = null;
        String amountCurrency = null;

        if (planId != null) {
            Map<String, Object> planRaw = paypalClient.get("/v1/billing/plans/" + planId, Map.class);
            System.out.println("RAW PLAN FROM PAYPAL = " + planRaw);

            Object billingCyclesObj = planRaw.get("billing_cycles");
            if (billingCyclesObj instanceof List<?> billingCycles && !billingCycles.isEmpty()) {
                Object firstCycleObj = billingCycles.get(0);
                if (firstCycleObj instanceof Map<?, ?> firstCycle) {
                    Object pricingSchemeObj = firstCycle.get("pricing_scheme");
                    if (pricingSchemeObj instanceof Map<?, ?> pricingScheme) {
                        Object fixedPriceObj = pricingScheme.get("fixed_price");
                        if (fixedPriceObj instanceof Map<?, ?> fixedPrice) {
                            Object valObj = fixedPrice.get("value");
                            Object curObj = fixedPrice.get("currency_code");
                            if (valObj instanceof String v) amountValue = v;
                            if (curObj instanceof String c) amountCurrency = c;
                        }
                    }
                }
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
                end,
                amountValue,
                amountCurrency
        );
    }
}
