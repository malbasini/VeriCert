package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.PaypalSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/paypal")
public class PaypalWebhookController {

    private final PaymentsProps.PaypalProps paypalProps;
    private final BillingService billingService;
    private final PaypalSubscriptionService paypalSubscriptionService;

    public PaypalWebhookController(PaymentsProps paymentsProps,
                                   BillingService billingService,
                                   PaypalSubscriptionService paypalSubscriptionService) {
        this.paypalProps = paymentsProps.getPaypal();
        this.billingService = billingService;
        this.paypalSubscriptionService = paypalSubscriptionService;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody Map<String, Object> body,
                                         @RequestHeader Map<String, String> headers) {

        // TODO: verificare la firma del webhook PayPal usando:
        // - headers: PAYPAL-TRANSMISSION-ID, PAYPAL-TRANSMISSION-TIME, PAYPAL-TRANSMISSION-SIG, PAYPAL-CERT-URL, PAYPAL-AUTH-ALGO
        // - body e paypalProps.getWebhookId()

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
        @SuppressWarnings("unchecked")
        Map<String, Object> resource = (Map<String, Object>) body.get("resource");
        if (resource == null) return;

        String subscriptionId = (String) resource.get("id");
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
}
