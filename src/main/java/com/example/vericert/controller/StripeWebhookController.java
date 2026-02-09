package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.*;
import com.example.vericert.service.*;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe/webhook")
public class StripeWebhookController {

    private final InvoiceService invoiceService;
    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final CertificateService certificateService;
    private final BillingService billingService;
    private final StripeSubscriptionService stripeSubscriptionService;
    private final TenantService tenantService;
    private final TenantSettingsService service;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final MailService mailService;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final TemplateRepository tempRepo;



    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    public StripeWebhookController(PaymentsProps props,
                                   PaymentRepository payRepo,
                                   CertificateService certificateService,
                                   BillingService billingService,
                                   StripeSubscriptionService stripeSubscriptionService,
                                   TenantService tenantService,
                                   TenantSettingsService tenantSettingsService,
                                   TenantSettingsRepository tenantSettingsRepo,
                                   MailService mailService,
                                   UserRepository userRepo,
                                   MembershipRepository membershipRepo,
                                   InvoiceService invoiceService,
                                   TemplateRepository tempRepo) {
        this.props = props;
        this.payRepo = payRepo;
        this.certificateService = certificateService;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.billingService = billingService;
        this.stripeSubscriptionService = stripeSubscriptionService;
        this.tenantService = tenantService;
        this.service = tenantSettingsService;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.mailService = mailService;
        this.userRepo = userRepo;
        this.membershipRepo = membershipRepo;
        this.invoiceService = invoiceService;
        this.tempRepo = tempRepo;
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
                    case "payment_intent.created" -> {
                        return (ResponseEntity<String>) ResponseEntity.status(200);
                    }
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

        String tenantIdStr  = m(metadata, "tenant_id", "tenantId");
        String planCode     = m(metadata, "plan_code", "planCode");
        String billingCycle = m(metadata, "billing_cycle", "billingCycle");

        if (tenantIdStr == null || planCode == null || billingCycle == null) {
            log.info(">>> Metadata mancanti, non aggiorno subscription. meta={}", metadata);
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
        //SEND MAIL
        String customerName = "";
        if (session.getCustomerDetails() != null && session.getCustomerDetails().getName() != null) {
            customerName = session.getCustomerDetails().getName();
        } else if (session.getCustomerEmail() != null) {
            customerName = session.getCustomerEmail(); // fallback
        } else if (session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
            customerName = session.getCustomerDetails().getEmail(); // fallback
        } else {
            customerName = ""; // ok
        }
        String to = null;
        if (session.getCustomerDetails() != null) {
            to = session.getCustomerDetails().getEmail();
        }
        if ((to == null || to.isBlank()) && session.getCustomerEmail() != null) {
            to = session.getCustomerEmail();
        }
        if (to == null || to.isBlank()) {
            TenantSettings ts = tenantSettingsRepo.findByTenantId(tenantId).orElse(null);
            if (ts != null) to = ts.getEmail();
        }
        String finalTo = to;
        // recupera payment e manda email una sola volta
        String finalPlanCode = planCode;
        String finalBillingCycle = billingCycle;
        String finalCustomerName = customerName;
        payRepo.findByCheckoutSessionId(session.getId()).ifPresent(p -> {
            if (p.isPurchaseEmailSentStripe()) return;
            long grossCents = session.getAmountTotal() != null ? session.getAmountTotal() : p.getAmountMinor();
            Map<String,Object> vars = new HashMap<>();
            vars.put("customer_name", finalCustomerName);
            vars.put("action", "Acquisto");
            vars.put("provider", "Stripe");
            vars.put("paid_at", formatRome(Instant.now()));
            vars.put("payment_ref", session.getId());// oppure session.getPaymentIntent()
            vars.put("plan_name", finalPlanCode);
            vars.put("billing_cycle", finalBillingCycle);
            vars.put("subscription_id", subscriptionId);
            vars.put("portal_url", "https://app.vercert.org/");
            vars.put("support_email", "support@app.vercert.org");
            vars.put("company_name", "VeriCert");
            vars.put("company_address", "…");
            vars.putAll(grossToVat22Vars(grossCents));
            mailService.sendPurchaseSuccess(finalTo, "Conferma pagamento - " + vars.getOrDefault("plan_name",finalPlanCode), vars);
            p.setPurchaseEmailSentStripe(true);
            p.setUpdatedAt(Instant.now());
            payRepo.save(p);
        });
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
            String tenantIdStr  = m(sub.getMetadata(), "tenant_id", "tenantId");
            if (tenantIdStr == null) {
                log.info("invoice.paid: tenant_id mancante nei metadata subscription %s".formatted(subId));
                return;
            }

            Long tenantId = Long.valueOf(tenantIdStr);
            TenantSettings ts = service.findPendingBySubscriptionId(tenantId).get();
            //CALCOLO DATE
        Instant now = Instant.now();

        Instant start = ts.getCurrentPeriodStart();
        Instant end   = ts.getCurrentPeriodEnd();

        Instant newStart;
        if (now.isBefore(start)) {
            // periodo assurdo nel futuro: riallinea
            newStart = now;
        } else if (now.isBefore(end)) {
            // siamo ancora nel periodo corrente: NON rinnovare ancora (skip)
            return;
        } else {
            // now >= end: rinnovo normale
            newStart = end;
        }
        Instant newEnd = computeNextPeriodEnd(newStart, ts.getBillingCycle());
        billingService.renewStripeSubscription(
                    tenantId,
                    ts.getPlanCode(),
                    ts.getBillingCycle(),
                    ts.getSubscriptionId(),
                    invoice.getId(),
                    newStart,
                    newEnd
            );
            //SEND EMAIL
            // dopo billingService.renewStripeSubscription(...)

            String customerName = "";
            if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()) {
                customerName = invoice.getCustomerName();
            }
            log.info(">>> Customer Name " + customerName);
            String to = ts.getEmail();
            if (to == null || to.isBlank()) {
                // fallback: invoice customer email (se presente)
                to = invoice.getCustomerEmail();
            }
            if (to == null || to.isBlank()) {
                TenantSettings t = tenantSettingsRepo.findByTenantId(tenantId).orElse(null);
                to = t.getEmail();
            }

            long grossCents = invoice.getTotal() != null ? invoice.getTotal() :
                    (invoice.getAmountPaid() != null ? invoice.getAmountPaid() : 0L);

             // salva/crea Payment del rinnovo e dedup email
            Payment pay = payRepo.findByProviderIntentId(invoice.getId()).orElseGet(() -> {
                Payment np = new Payment();
                np.setTenantId(tenantId);
                np.setProvider("STRIPE");
                np.setProviderIntentId(invoice.getId()); // invoice id
                np.setStatus("SUCCEEDED");
                np.setAmountMinor(grossCents);
                np.setCurrency("EUR");
                np.setIdempotencyKey("STRIPE:INVOICE_PAID:" + invoice.getId());
                np.setCreatedAt(Instant.now());
                np.setUpdatedAt(Instant.now());
                return np;
            });

            if (pay.getId() == null) {
                try {
                    pay = payRepo.saveAndFlush(pay);
                } catch (DataIntegrityViolationException dup) {
                    // già registrato altrove
                    pay = payRepo.findByProviderIntentId(invoice.getId()).orElseThrow();
                }
            }

            if (pay.isRenewEmailSentStripe()) return;

            // paid_at (meglio della data di sistema)
            Instant paidAt = Instant.now();
            if (invoice.getStatusTransitions() != null && invoice.getStatusTransitions().getPaidAt() != null) {
                paidAt = Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt());
            }

            Map<String,Object> vars = new HashMap<>();
            vars.put("action", "Rinnovo");
            vars.put("customer_name", customerName);
            vars.put("provider", "Stripe");
            vars.put("plan_name", ts.getPlanCode());
            vars.put("billing_cycle", ts.getBillingCycle());
            vars.put("payment_ref", invoice.getId());
            vars.put("subscription_id", subId);
            vars.put("paid_at", formatRome(paidAt));
            vars.put("portal_url", "https://app.vercert.org/");
            vars.put("support_email", "support@app.vercert.org");
            vars.put("company_name", "VeriCert");
            vars.put("company_address", "…");
            vars.putAll(grossToVat22Vars(grossCents));
            mailService.sendPurchaseSuccess(to, "Conferma pagamento - " + ts.getPlanCode(), vars);
            pay.setRenewEmailSentStripe(true);
            pay.setUpdatedAt(Instant.now());
            payRepo.save(pay);

    }
    private Instant computeNextPeriodEnd(Instant start, String cycle) {
        ZonedDateTime z = start.atZone(ZoneId.of("Europe/Rome"));
        return switch (String.valueOf(cycle).toUpperCase()) {
            case "YEARLY", "ANNUAL", "ANNUALLY" -> z.plusYears(1).toInstant();
            default -> z.plusMonths(1).toInstant(); // MONTHLY
        };
    }
    private static Map<String, String> grossToVat22Vars(long grossCents) {
        BigDecimal gross = BigDecimal.valueOf(grossCents, 2);          // 0.61
        BigDecimal net = gross.divide(new BigDecimal("1.22"), 2, RoundingMode.HALF_UP);
        BigDecimal vat = gross.subtract(net).setScale(2, RoundingMode.HALF_UP);

        Map<String,String> out = new HashMap<>();
        out.put("amount_total", formatEuroIT(gross));
        out.put("amount_net",   formatEuroIT(net));
        out.put("vat_amount",   formatEuroIT(vat));
        return out;
    }

    public static String formatEuroIT(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ITALY);
        nf.setCurrency(Currency.getInstance("EUR"));
        return nf.format(value);
    }

    private static String formatRome(Instant instant) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Rome"))
                .format(instant);
    }
    private static String m(Map<String, String> meta, String... keys) {
        if (meta == null) return null;
        for (String k : keys) {
            String v = meta.get(k);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }


}
