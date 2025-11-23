package com.example.vericert.controller;

import com.example.vericert.service.BillingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks")
public class BillingWebhookController {

    private final BillingService billingService;

    public BillingWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/stripe")
    public void handleStripe(@RequestBody String payload,
                             @RequestHeader("Stripe-Signature") String signatureHeader) {
        // TODO: verifica firma + parse evento con Stripe SDK

        // Esempio logico (pseudo-codice):
        // if event.type == "checkout.session.completed" || "customer.subscription.updated"
        //    Long tenantId = Long.valueOf(event.getMetadata("tenant_id"));
        //    String planCode = event.getMetadata("plan_code");
        //    String billingCycle = event.getMetadata("billing_cycle");
        //    String subscriptionId = event.getSubscriptionId();
        //    String invoiceId = event.getLastInvoiceId();
        //    billingService.activateSubscription(tenantId, planCode, billingCycle,
        //        BillingProvider.STRIPE, subscriptionId, invoiceId);
        //
        // if event.type == "invoice.payment_failed" ecc.
        //    billingService.markPastDue(tenantId);
    }

    @PostMapping("/paypal")
    public void handlePaypal(@RequestBody String payload) {
        // TODO: verifica webhook PayPal, estrae tenantId, planCode, billingCycle, status
        // in caso di APPROVED/ACTIVE
        // billingService.activateSubscription(..., BillingProvider.PAYPAL, ...);
        // in caso di CANCELLED / SUSPENDED
        // billingService.cancelSubscription(tenantId);
    }
}
