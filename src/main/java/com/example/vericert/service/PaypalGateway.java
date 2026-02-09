package com.example.vericert.service;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.component.PaypalClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaypalGateway {

    private final PaypalClient paypalClient;
    private final PaymentsProps.PaypalProps props;

    public PaypalGateway(PaypalClient paypalClient, PaymentsProps paymentsProps) {
        this.paypalClient = paypalClient;
        this.props = paymentsProps.getPaypal();
    }

    /**
     * Crea una subscription PayPal e ritorna l'URL di approvazione
     * su cui fare redirect.
     *
     * @param paypalPlanId ID del piano PayPal (per MONTHLY/ANNUAL lo prendi da PlanDefinition)
     */
    public String createSubscription(
            Long tenantId,
            String planCode,
            String billingCycle,
            String paypalPlanId,
            String successUrl,
            String cancelUrl
    ) {
        // custom_id con info del tenant
        Map<String, String> customId = Map.of(
                "tenant_id", tenantId.toString(),
                "plan_code", planCode,
                "billing_cycle", billingCycle
        );

        Map<String, Object> body = new HashMap<>();
        body.put("plan_id", paypalPlanId);
        body.put("custom_id", toJson(customId));

        Map<String, Object> applicationContext = new HashMap<>();
        applicationContext.put("brand_name", "VeriCert");
        applicationContext.put("user_action", "SUBSCRIBE_NOW");
        applicationContext.put("return_url", successUrl);
        applicationContext.put("cancel_url", cancelUrl);

        body.put("application_context", applicationContext);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = paypalClient.post("/v1/billing/subscriptions", body, Map.class);

        String id = (String) response.get("id");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> links = (List<Map<String, String>>) response.get("links");

        String approvalUrl = links.stream()
                .filter(l -> "approve".equalsIgnoreCase(l.get("rel")))
                .map(l -> l.get("href"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessun link approve per subscription " + id));

        return approvalUrl;
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
