package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;


@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {


    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final CertificateService certificateService; // se vuoi emettere/sbloccare qui
    private final AdminPlanDefinitionsService service;
    private final BillingService billingService;

    public StripeWebhookController(PaymentsProps props,
                                   PaymentRepository payRepo ,
                                   CertificateService certificateService,
                                   AdminPlanDefinitionsService service,
                                   BillingService billingService) {
        this.props = props;
        this.payRepo = payRepo;
        this.certificateService = certificateService;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.service = service;
        this.billingService = billingService;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestHeader("Stripe-Signature") String sig,
                                         @RequestBody String payload) {
        try {
            var event = com.stripe.net.Webhook.constructEvent(payload, sig, props.getStripe().getWebhookSecret());
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    var session = (com.stripe.model.checkout.Session) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    payRepo.findByCheckoutSessionId(session.getId()).ifPresent(p -> {
                        if (!"SUCCEEDED".equals(p.getStatus())) {
                            p.setStatus("SUCCEEDED");
                            if (p.getProviderIntentId() == null && session.getPaymentIntent() != null) {
                                p.setProviderIntentId(session.getPaymentIntent());
                            }
                            p.setUpdatedAt(Instant.now());
                            payRepo.save(p);
                            if (p.getCertificateId()!=null) certificateService.unlockOrIssue();
                        }
                    });
                    if (session != null) {
                        String tenantIdStr = session.getMetadata().get("tenant_id");
                        String planCode = session.getMetadata().get("plan_code");
                        String billingCycle = session.getMetadata().get("billing_cycle");

                        Long tenantId = Long.valueOf(tenantIdStr);
                        String subscriptionId = session.getSubscription();
                        String invoiceId = session.getInvoice();

                        // Qui attivi davvero il piano nel tuo DB
                        billingService.activateSubscription(
                                tenantId,
                                planCode,
                                billingCycle,
                                BillingProvider.STRIPE,
                                subscriptionId,
                                invoiceId
                        );
                    }
                }
                case "payment_intent.payment_failed" -> {
                    var pi = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    payRepo.findByProviderIntentId(pi.getId()).ifPresent(p -> {
                        p.setStatus("FAILED");
                        p.setUpdatedAt(Instant.now());
                        payRepo.save(p);
                    });
                }
                default -> {
                // altri eventi ignorati
                }
            }
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(400).body("bad signature");
        }
    }
}