package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import com.example.vericert.service.BillingService;
import com.example.vericert.service.CertificateService;
import com.example.vericert.service.StripeSubscriptionService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final CertificateService certificateService;
    private final BillingService billingService;
    private final StripeSubscriptionService stripeSubscriptionService;

    public StripeWebhookController(PaymentsProps props,
                                   PaymentRepository payRepo,
                                   CertificateService certificateService,
                                   AdminPlanDefinitionsService service,
                                   BillingService billingService,
                                   StripeSubscriptionService stripeSubscriptionService) {
        this.props = props;
        this.payRepo = payRepo;
        this.certificateService = certificateService;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.billingService = billingService;
        this.stripeSubscriptionService = stripeSubscriptionService;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestHeader("Stripe-Signature") String sig,
                                         @RequestBody String payload) {
        try {
            var event = com.stripe.net.Webhook.constructEvent(
                    payload,
                    sig,
                    props.getStripe().getWebhookSecret()
            );

            String type = event.getType();
            System.out.println(">>> STRIPE EVENT = " + type);

            switch (type) {
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
                case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
                case "invoice.paid" -> handleInvoicePaid(event);
                case "payment_intent.created" -> handleInvoiceIntentCreate(event);
                default -> {
                    // tutti gli altri (payment_method.*, charge.*, ecc.) li puoi ignorare
                    System.out.println(">>> Evento ignorato: " + type);
                }
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body("bad signature");
        }
    }

    private void handleInvoiceIntentCreate(Event event) throws EventDataObjectDeserializationException {

        var deserializer = event.getDataObjectDeserializer();
        var stripeObject = deserializer.deserializeUnsafe();

        if (!(stripeObject instanceof com.stripe.model.PaymentIntent pi)) {
            return;
        }
        // Se esiste già un Payment con questo intent, non duplicare
        if (payRepo.findByProviderIntentId(pi.getId()).isPresent()) {
            return;
        }
        Payment p = new Payment();
        p.setProvider("STRIPE");
        p.setProviderIntentId(pi.getId());
        p.setStatus("PENDING");
        p.setCreatedAt(Instant.now());
        // se nei metadata del PaymentIntent hai tenant_id/certificate_id, li puoi leggere qui:
        String tenantIdStr = pi.getMetadata().get("tenant_id");
        p.setTenantId(Long.valueOf(tenantIdStr));
        payRepo.save(p);






    }

    private void handleCheckoutSessionCompleted(com.stripe.model.Event event) {
        var deserializer = event.getDataObjectDeserializer();

        com.stripe.model.StripeObject stripeObject;
        try {
            stripeObject = deserializer.getObject()
                    .orElseGet(() -> {
                        try {
                            return deserializer.deserializeUnsafe();
                        } catch (EventDataObjectDeserializationException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            System.out.println("⚠️ Impossibile deserializzare checkout.session: " + e.getMessage());
            System.out.println("RAW JSON = " + deserializer.getRawJson());
            return;
        }

        if (!(stripeObject instanceof com.stripe.model.checkout.Session session)) {
            System.out.println("⚠️ data.object NON è una checkout.Session ma "
                    + stripeObject.getClass().getName());
            System.out.println("RAW JSON = " + deserializer.getRawJson());
            return;
        }

        System.out.println("SESSION ID = " + session.getId());
        System.out.println("METADATA  = " + session.getMetadata());

        // --- blocco PaymentRepository, se ti serve ---
        payRepo.findByCheckoutSessionId(session.getId()).ifPresent(p -> {
            if (!"SUCCEEDED".equals(p.getStatus())) {
                p.setStatus("SUCCEEDED");
                if (p.getProviderIntentId() == null && session.getPaymentIntent() != null) {
                    p.setProviderIntentId(session.getPaymentIntent());
                }
                p.setUpdatedAt(Instant.now());
                payRepo.save(p);
                if (p.getCertificateId() != null) {
                    certificateService.unlockOrIssue();
                }
            }
        });

        // --- blocco attivazione piano per il tenant ---
        var metadata = session.getMetadata();
        String tenantIdStr   = metadata.get("tenant_id");
        String planCode      = metadata.get("plan_code");
        String billingCycle  = metadata.get("billing_cycle");

        if (tenantIdStr == null || planCode == null || billingCycle == null) {
            System.out.println(">>> Metadata mancanti, non aggiorno subscription.");
            return;
        }

        Long tenantId = Long.valueOf(tenantIdStr);
        String subscriptionId = session.getSubscription();
        String invoiceId      = session.getInvoice();  // può essere null

        billingService.activateSubscription(
                tenantId,
                planCode,
                billingCycle,
                BillingProvider.STRIPE,
                subscriptionId,
                invoiceId
        );

        System.out.println(">>> Attivata subscription per tenant " + tenantId);
    }
    private void handlePaymentIntentFailed(com.stripe.model.Event event) throws EventDataObjectDeserializationException {
        var deserializer = event.getDataObjectDeserializer();
        var stripeObject = deserializer.deserializeUnsafe();

        if (!(stripeObject instanceof com.stripe.model.PaymentIntent pi)) {
            return;
        }

        payRepo.findByProviderIntentId(pi.getId()).ifPresent(p -> {
            p.setStatus("FAILED");
            p.setUpdatedAt(Instant.now());
            payRepo.save(p);
        });
    }

    private void handleInvoicePaid(com.stripe.model.Event event) throws Exception {
        var deserializer = event.getDataObjectDeserializer();
        var stripeObject = deserializer.deserializeUnsafe();

        if (!(stripeObject instanceof com.stripe.model.Invoice invoice)) {
            return;
        }

        if (invoice.getSubscription() == null) {
            return;
        }

        String subscriptionId = invoice.getSubscription();
        var stripeSub = stripeSubscriptionService.findById(subscriptionId);

        String tenantIdStr  = stripeSub.getMetadata().get("tenant_id");
        String planCode     = stripeSub.getMetadata().get("plan_code");
        String billingCycle = stripeSub.getMetadata().get("billing_cycle");

        if (tenantIdStr == null) {
            return;
        }

        Long tenantId = Long.valueOf(tenantIdStr);

        long startEpoch = stripeSub.getCurrentPeriodStart();
        long endEpoch   = stripeSub.getCurrentPeriodEnd();

        billingService.renewStripeSubscription(
                tenantId,
                planCode,
                billingCycle,
                subscriptionId,
                invoice.getId(),
                startEpoch,
                endEpoch
        );
    }
}
