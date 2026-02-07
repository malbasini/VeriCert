package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.component.PaypalClient;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.exception.DuplicateWebhookEventException;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PaypalSubscriptionService;
import com.example.vericert.service.WebookEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/paypal/webhook")
public class PaypalWebhookController {


    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final WebookEventService processedEventService;
    private final PaymentsProps.PaypalProps paypalProps;
    private final BillingService billingService;
    private final PaypalSubscriptionService paypalSubscriptionService;
    private final PaypalClient paypalClient; // lo hai già per le altre chiamate
    // per webhookId


    public PaypalWebhookController(PaymentsProps paymentsProps,
                                   BillingService billingService,
                                   PaypalSubscriptionService paypalSubscriptionService,
                                   PaypalClient paypalClient,
                                   WebookEventService processedEventService) {
        this.paypalProps = paymentsProps.getPaypal();
        this.billingService = billingService;
        this.paypalSubscriptionService = paypalSubscriptionService;
        this.paypalClient = paypalClient;
        this.processedEventService = processedEventService;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody String rawBody,
                                         @RequestHeader Map<String, String> headers) throws IOException {

        System.out.println("RAW BODY DENTRO WEBHOOK = " + rawBody);
        // 1) Verifica firma via API PayPal
        if (!verifyWithPaypalApi(headers, rawBody)) {
            return ResponseEntity.status(200).body("invalid paypal signature");
        }
        System.out.println("DENTRO WEBHOOK DOPO VERIFICA FIRMA");
        // 2) Deserializza il body in Map SOLO dopo che la firma è ok
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> body = om.readValue(rawBody, Map.class);
        String eventId = (String) body.get("id");
        String eventType = (String) body.get("event_type");
        if (eventId == null) return ResponseEntity.ok("ok");
        try {
            processedEventService.markProcessed(BillingProvider.PAYPAL, eventId, eventType);
        } catch (DuplicateWebhookEventException e) {
            return ResponseEntity.ok("duplicate");
        }
        log.info(new StringBuilder().append(">>> PAYPAL EVENT = ").append(eventType).toString());
        switch (eventType) {
            case "BILLING.SUBSCRIPTION.ACTIVATED" -> handleSubscriptionActivated(body, headers);
            case "PAYMENT.SALE.COMPLETED" -> handlePaymentSaleCompleted(body);
            case "BILLING.SUBSCRIPTION.CANCELLED" -> handleSubscriptionCancelled(body);
            default -> log.info(new StringBuilder().append(">>> Evento PayPal ignorato: ").append(eventType).toString());
        }
        return ResponseEntity.ok("ok");
    }

    private void handleSubscriptionActivated(Map<String, Object> body, Map<String, String> headers) {
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        System.out.println("HEADERS KEYS = " + headers.keySet());
        if (resource == null) return;

        String subscriptionId = extractSubscriptionId(resource);
        String customId = (String) resource.get("custom_id"); // <-- è una STRINGA JSON

        if (subscriptionId == null || customId == null) {
            log.info(">>> [PayPal] missing id/custom_id");
            return;
        }

        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(customId);
            Long tenantId = node.path("tenant_id").isMissingNode() ? null : node.path("tenant_id").asLong();
            String planCode = node.path("plan_code").asText(null);
            String billingCycle = node.path("billing_cycle").asText(null);
            if (tenantId == null || planCode == null || billingCycle == null) {
                log.info(">>> [PayPal] ACTIVATED tenantId={} planCode={} billingCycle={} subscriptionId={}", tenantId, planCode, billingCycle, subscriptionId);
                return;
            }
            billingService.activateSubscription(
                    tenantId,
                    planCode,
                    billingCycle,
                    BillingProvider.PAYPAL,
                    subscriptionId,
                    null
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePaymentSaleCompleted(Map<String, Object> body) {
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        if (resource == null) return;
        // Nelle notifiche subscription, di solito hai "billing_agreement_id" o "subscription_id"
        String subscriptionId = extractSubscriptionId(resource);
        if (subscriptionId == null) {
            // in alcuni casi è nested in "supplementary_data" / "related_ids"
            Map<String, Object> supplementary = (Map<String, Object>) resource.get("supplementary_data");
            if (supplementary != null) {
                Map<String, Object> relatedIds = (Map<String, Object>) supplementary.get("related_ids");
                if (relatedIds != null) {
                    subscriptionId = (String) relatedIds.get("billing_agreement_id");
                }
            }
        }
        if (subscriptionId == null) {
            log.info(">>> PAYMENT.SALE.COMPLETED senza subscriptionId, ignorato.");
            return;
        }
        var dto = paypalSubscriptionService.findById(subscriptionId);
        if (dto.tenantId() == null || dto.planCode() == null || dto.billingCycle() == null) {
            log.info(">>> Subscription PayPal senza custom_id valido, non rinnovo.");
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
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        if (resource == null) return;
        String subscriptionId = extractSubscriptionId(resource);
        var dto = paypalSubscriptionService.findById(subscriptionId);
        if (dto.tenantId() == null) {
            return;
        }
        Long tenantId = Long.valueOf(dto.tenantId());
        billingService.cancelSubscription(tenantId);
    }

    private static String h(Map<String, String> headers, String name) {
        if (headers == null) return null;
        // crea una vista case-insensitive (lowercase)
        for (var e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean verifyWithPaypalApi(Map<String, String> headers, String rawBody) {
        String transmissionId = h(headers, "PAYPAL-TRANSMISSION-ID");
        String transmissionTime = h(headers, "PAYPAL-TRANSMISSION-TIME");
        String transmissionSig = h(headers, "PAYPAL-TRANSMISSION-SIG");
        String certUrl = h(headers, "PAYPAL-CERT-URL");
        String authAlgo = h(headers, "PAYPAL-AUTH-ALGO");

        if (transmissionId == null || transmissionTime == null || transmissionSig == null
                || certUrl == null || authAlgo == null) {
            return false;
        }

        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> event = om.readValue(rawBody, Map.class);

            Map<String, Object> req = new HashMap<>();
            req.put("transmission_id", transmissionId);
            req.put("transmission_time", transmissionTime);
            req.put("cert_url", certUrl);
            req.put("auth_algo", authAlgo);
            req.put("transmission_sig", transmissionSig);
            req.put("webhook_id", paypalProps.getWebhookId());
            req.put("webhook_event", event);

            Map<String, Object> resp = paypalClient.post(
                    "/v1/notifications/verify-webhook-signature",
                    req,
                    Map.class
            );

            String status = (String) resp.get("verification_status");
            return "SUCCESS".equalsIgnoreCase(status);

        } catch (Exception ex) {
            // log.warn("PayPal signature verification failed", ex);
            return false;
        }
    }
    private String extractSubscriptionId(Map<String, Object> resource) {
        String id = (String) resource.get("billing_agreement_id");
        if (id == null) id = (String) resource.get("subscription_id");

        if (id == null) {
            Map<String, Object> supplementary = (Map<String, Object>) resource.get("supplementary_data");
            if (supplementary != null) {
                Map<String, Object> relatedIds = (Map<String, Object>) supplementary.get("related_ids");
                if (relatedIds != null) {
                    id = (String) relatedIds.get("billing_agreement_id");
                    if (id == null) id = (String) relatedIds.get("subscription_id");
                }
            }
        }
        return id;
    }

}

