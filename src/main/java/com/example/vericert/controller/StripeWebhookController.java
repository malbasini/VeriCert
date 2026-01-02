package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.*;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {

    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final CertificateService certificateService;
    private final BillingService billingService;
    private final StripeSubscriptionService stripeSubscriptionService;
    private final TenantService tenantService;
    private final TenantSettingsService service;

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    public StripeWebhookController(PaymentsProps props,
                                   PaymentRepository payRepo,
                                   CertificateService certificateService,
                                   BillingService billingService,
                                   StripeSubscriptionService stripeSubscriptionService,
                                   TenantService tenantService,
                                   TenantSettingsService tenantSettingsService) {
        this.props = props;
        this.payRepo = payRepo;
        this.certificateService = certificateService;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.billingService = billingService;
        this.stripeSubscriptionService = stripeSubscriptionService;
        this.tenantService = tenantService;
        this.service = tenantSettingsService;
    }

    @PostMapping
    public ResponseEntity<String> handle(@RequestHeader("Stripe-Signature") String sig,
                                         @RequestBody String payload) {
        try {
            final Event event;
            try {
                event = Webhook.constructEvent(payload, sig, props.getStripe().getWebhookSecret());
            } catch (SignatureVerificationException e) {
                // QUI è davvero firma errata
                return ResponseEntity.status(400).body("invalid signature");
            } catch (Exception e) {
                // payload malformato ecc.
                e.printStackTrace();
                return ResponseEntity.status(400).body("bad request");
            }
            String type = event.getType();
            log.info(">>> STRIPE EVENT = " + type);
            try {
                switch (type) {
                    case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
                    case "invoice.paid" -> handleInvoicePaid(event);
                    case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
                    default -> System.out.println(">>> Evento ignorato: " + type);
                }
            } catch (Exception ex) {
                // questo NON è una firma errata
                ex.printStackTrace();

                // scelta: se vuoi che Stripe ritenti per eventi critici:
                if ("invoice.paid".equals(type) || "checkout.session.completed".equals(type)) {
                    return ResponseEntity.status(500).body("processing error");
                }
                // per tutto il resto, rispondi 200 e basta
                return ResponseEntity.ok("ok");
            }

            return ResponseEntity.ok("ok");
        }
        finally {}
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
            log.info("⚠\uFE0F Impossibile deserializzare checkout.session: {}", e.getMessage());
            log.info(new StringBuilder().append("RAW JSON = ").append(deserializer.getRawJson()).toString());
            return;
        }

        if (!(stripeObject instanceof com.stripe.model.checkout.Session session)) {
            log.info("\u26A0\uFE0F data.object NON \u00E8 una checkout.Session ma "
                    + stripeObject.getClass().getName());
            log.info(new StringBuilder().append("RAW JSON = ").append(deserializer.getRawJson()).toString());
            return;
        }

        log.info("SESSION ID = %s".formatted(session.getId()));
        log.info("METADATA  = {}", session.getMetadata());

        // --- blocco PaymentRepository, se ti serve ---
        payRepo.findByCheckoutSessionId(session.getId()).ifPresent(p -> {
            if (!"SUCCEEDED".equals(p.getStatus())) {
                p.setStatus(PlanStatus.SUCCEDED.name());
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
            log.info(">>> Metadata mancanti, non aggiorno subscription.");
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

        log.info(">>> Attivata subscription per tenant %d".formatted(tenantId));
    }
    private void handlePaymentIntentFailed(com.stripe.model.Event event) throws EventDataObjectDeserializationException {
        var deserializer = event.getDataObjectDeserializer();
        var stripeObject = deserializer.deserializeUnsafe();

        if (!(stripeObject instanceof com.stripe.model.PaymentIntent pi)) {
            return;
        }

        payRepo.findByProviderIntentId(pi.getId()).ifPresent(p -> {
            p.setStatus(PlanStatus.CANCELED.name());
            p.setUpdatedAt(Instant.now());
            payRepo.save(p);
            billingService.markPastDue(p.getTenantId());
        });
    }

    private void handleInvoicePaid(Event event) throws StripeException {

            Invoice invoice = (Invoice) event.getDataObjectDeserializer().deserializeUnsafe();
            if (invoice.getSubscription() == null) return;

            String subId = invoice.getSubscription();

            com.stripe.model.Subscription sub = com.stripe.model.Subscription.retrieve(subId);
            String tenantIdStr = sub.getMetadata().get("tenant_id");

            if (tenantIdStr == null) {
                log.info("invoice.paid: tenant_id mancante nei metadata subscription %s".formatted(subId));
                return;
            }

            Long tenantId = Long.valueOf(tenantIdStr);

            TenantSettings ts = service.findPendingBySubscriptionId(tenantId).get();

            billingService.renewStripeSubscription(
                    tenantId,
                    ts.getPlanCode(),
                    ts.getBillingCycle(),
                    ts.getSubscriptionId(),
                    invoice.getId(),
                    sub.getCurrentPeriodStart(),
                    sub.getCurrentPeriodEnd()
            );
        }















}
