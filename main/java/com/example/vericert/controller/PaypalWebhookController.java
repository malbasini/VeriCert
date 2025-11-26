package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.component.PaypalClient;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PaypalSubscriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/paypal")
public class PaypalWebhookController {

    private final PaymentsProps.PaypalProps paypalProps;
    private final BillingService billingService;
    private final PaypalSubscriptionService paypalSubscriptionService;
    private final PaypalClient paypalClient; // lo hai già per le altre chiamate
    // per webhookId


    public PaypalWebhookController(PaymentsProps paymentsProps,
                                   BillingService billingService,
                                   PaypalSubscriptionService paypalSubscriptionService,
                                   PaypalClient paypalClient) {
        this.paypalProps = paymentsProps.getPaypal();
        this.billingService = billingService;
        this.paypalSubscriptionService = paypalSubscriptionService;
        this.paypalClient = paypalClient;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody String rawBody,
                                         @RequestHeader Map<String, String> headers) throws IOException {

        // TODO: verificare la firma del webhook PayPal usando:
        // - headers: PAYPAL-TRANSMISSION-ID, PAYPAL-TRANSMISSION-TIME, PAYPAL-TRANSMISSION-SIG, PAYPAL-CERT-URL, PAYPAL-AUTH-ALGO
        // - body e paypalProps.getWebhookId()

        System.out.println("RAW BODY: " + rawBody);

        // 1) Verifica firma via API PayPal
        if (!verifyWithPaypalApi(headers, rawBody)) {
            return ResponseEntity.status(400).body("invalid paypal signature");
        }

        // 2) Deserializza il body in Map SOLO dopo che la firma è ok
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> body = om.readValue(rawBody, Map.class);

        String eventType = (String) body.get("event_type");
        System.out.println(">>> PAYPAL EVENT = " + eventType);

        switch (eventType) {
            case "BILLING.SUBSCRIPTION.ACTIVATED" -> handleSubscriptionActivated(body);
            case "PAYMENT.SALE.COMPLETED" -> handlePaymentSaleCompleted(body);
            case "BILLING.SUBSCRIPTION.CANCELLED" -> handleSubscriptionCancelled(body);
            default -> System.out.println(">>> Evento PayPal ignorato: " + eventType);
        }

        return ResponseEntity.ok("ok");
    }

    private void handleSubscriptionActivated(Map<String, Object> body) {
        Object resourceObj = body.get("resource");
        if (!(resourceObj instanceof Map<?, ?> resource)) {
            System.out.println(">>> [PayPal] resource NON è una Map in BILLING.SUBSCRIPTION.ACTIVATED");
            System.out.println(">>> resource class = " + (resourceObj == null ? "null" : resourceObj.getClass().getName()));
            System.out.println(">>> BODY = " + body);
            return; // evito il ClassCastException
        }
        Object idObj = resource.get("id");
        if (!(idObj instanceof String subscriptionId)) {
            System.out.println(">>> [PayPal] resource.id non è una String");
            System.out.println(">>> id class = " + (idObj == null ? "null" : idObj.getClass().getName()));
            System.out.println(">>> resource = " + resource);
            return;
        }

        System.out.println(">>> [PayPal] Subscription ACTIVATED, id = " + subscriptionId);
        var dto = paypalSubscriptionService.findById(subscriptionId);
        if (dto.tenantId() == null || dto.planCode() == null || dto.billingCycle() == null) {
            System.out.println(">>> Subscription PayPal senza custom_id valido, non attivo piano.");
            return;
        }
        Long tenantId = Long.valueOf(dto.tenantId());
        billingService.activateSubscription(
                tenantId,
                dto.planCode(),
                dto.billingCycle(),
                BillingProvider.PAYPAL,
                dto.id(),
                null
        );
    }
    private void handlePaymentSaleCompleted(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        if (resource == null) return;
        // Nelle notifiche subscription, di solito hai "billing_agreement_id" o "subscription_id"
        String subscriptionId = (String) resource.get("billing_agreement_id");
        if (subscriptionId == null) {
            // in alcuni casi è nested in "supplementary_data" / "related_ids"
            @SuppressWarnings("unchecked")
            Map<String, Object> supplementary = (Map<String, Object>) resource.get("supplementary_data");
            if (supplementary != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> relatedIds = (Map<String, Object>) supplementary.get("related_ids");
                if (relatedIds != null) {
                    subscriptionId = (String) relatedIds.get("billing_agreement_id");
                }
            }
        }
        if (subscriptionId == null) {
            System.out.println(">>> PAYMENT.SALE.COMPLETED senza subscriptionId, ignorato.");
            return;
        }
        var dto = paypalSubscriptionService.findById(subscriptionId);
        if (dto.tenantId() == null || dto.planCode() == null || dto.billingCycle() == null) {
            System.out.println(">>> Subscription PayPal senza custom_id valido, non rinnovo.");
            return;
        }
        Long tenantId = Long.valueOf(dto.tenantId());
        // Id della transazione PayPal
        String transactionId = (String) resource.get("id");
        Instant start = dto.currentPeriodStart();
        Instant end = dto.currentPeriodEnd();
        billingService.renewPaypalSubscription(
                tenantId,
                dto.planCode(),
                dto.billingCycle(),
                dto.id(),           // providerSubscriptionId
                transactionId,      // lastInvoiceId / transactionId
                start,
                end
        );
    }
    private void handleSubscriptionCancelled(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        if (resource == null) return;
        String subscriptionId = (String) resource.get("id");
        var dto = paypalSubscriptionService.findById(subscriptionId);
        if (dto.tenantId() == null) {
            return;
        }
        Long tenantId = Long.valueOf(dto.tenantId());
        billingService.cancelSubscription(tenantId);
    }
    private boolean verifyWithPaypalApi(Map<String, String> headers, String rawBody) throws JsonProcessingException {
        // PayPal potrebbe passarti header in minuscolo o maiuscolo, gestiamoli entrambi
        String transmissionId   = headers.getOrDefault("paypal-transmission-id", headers.get("PAYPAL-TRANSMISSION-ID"));
        String transmissionTime = headers.getOrDefault("paypal-transmission-time", headers.get("PAYPAL-TRANSMISSION-TIME"));
        String transmissionSig  = headers.getOrDefault("paypal-transmission-sig", headers.get("PAYPAL-TRANSMISSION-SIG"));
        String certUrl          = headers.getOrDefault("paypal-cert-url", headers.get("PAYPAL-CERT-URL"));
        String authAlgo         = headers.getOrDefault("paypal-auth-algo", headers.get("PAYPAL-AUTH-ALGO"));

        if (transmissionId == null || transmissionTime == null || transmissionSig == null
                || certUrl == null || authAlgo == null) {
            System.out.println(">>> Missing PayPal signature headers");
            return false;
        }
        // Request ufficiale PayPal verify-webhook-signature
        java.util.Map<String, Object> verifyRequest = new java.util.HashMap<>();
        verifyRequest.put("transmission_id", transmissionId);
        verifyRequest.put("transmission_time", transmissionTime);
        verifyRequest.put("cert_url", certUrl);
        verifyRequest.put("auth_algo", authAlgo);
        verifyRequest.put("transmission_sig", transmissionSig);
        verifyRequest.put("webhook_id", paypalProps.getWebhookId()); // quello che hai in application.yml
        verifyRequest.put("webhook_event", new com.fasterxml.jackson.databind.ObjectMapper().readValue(rawBody, java.util.Map.class));
        // Chiamata a /v1/notifications/verify-webhook-signature
        Map<String, Object> response = paypalClient.post(
                "/v1/notifications/verify-webhook-signature",
                verifyRequest,
                Map.class
        );

        String status = (String) response.get("verification_status");
        System.out.println(">>> PayPal verify-webhook-signature: " + status);
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
