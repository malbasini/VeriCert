package com.example.vericert.service;

import com.example.vericert.component.PaypalClient;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanDefinitionRepository planDefinitionRepo;
    private final StripeGateway stripeGateway;
    private final TenantRepository tenantRepo;
    private final PaypalGateway  paypalGateway;
    private final PlanUsageResetService planUsageResetService;
    private final PaypalClient paypalClient;
    private final PaymentRepository paymentRepo;
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    public BillingService(TenantSettingsRepository tenantSettingsRepo,
                          PlanDefinitionRepository planDefinitionRepo,
                          StripeGateway stripeGateway,
                          TenantRepository tenantRepo,
                          PaypalGateway paypalGateway,
                          PlanUsageResetService planUsageResetService,
                          PaypalClient paypalClient,
                          PaymentRepository paymentRepo) {

        this.tenantSettingsRepo = tenantSettingsRepo;
        this.planDefinitionRepo = planDefinitionRepo;
        this.stripeGateway = stripeGateway;
        this.tenantRepo = tenantRepo;
        this.paypalGateway = paypalGateway;
        this.planUsageResetService = planUsageResetService;
        this.paypalClient = paypalClient;
        this.paymentRepo = paymentRepo;
    }
    /**
     * Avvia un checkout per il tenant, scegliendo provider e ciclo.
     * Ritorna l'URL su cui il frontend deve fare redirect.
     */
    @Transactional
    public String startCheckout(Long tenantId,
                                String planCode,
                                String billingCycle,       // "MONTHLY" o "ANNUAL"
                                BillingProvider provider) {

        boolean annual = "ANNUAL".equalsIgnoreCase(billingCycle);

        PlanDefinition plan = planDefinitionRepo.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Piano non trovato: " + planCode));

        TenantSettings settings = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        log.info("startCheckout: tenantId={} planCode={} billingCycle={} provider={} annual={} currentStatus={} currentProvider={} currentPlanCode={}",
                tenantId,
                planCode,
                billingCycle,
                provider,
                annual,
                settings.getStatus(),
                settings.getProvider(),
                settings.getPlanCode()
        );

        try {
            if (provider == BillingProvider.STRIPE) {

                String priceId = resolvePriceId(plan, billingCycle, provider);
                log.info("startCheckout STRIPE: resolved priceId={}", priceId);

                Session session = stripeGateway.createCheckoutSession(tenantId, priceId, planCode, billingCycle);
                log.info("Stripe checkoutSession created: id={} url={}", session.getId(), session.getUrl());

                // Aggiorno settings
                settings.setPlanCode(planCode);
                settings.setBillingCycle(billingCycle);
                settings.setProvider(provider.name());
                settings.setCheckoutSessionId(session.getId());
                settings.setStatusEnum(PlanStatus.TRIALING);
                tenantSettingsRepo.save(settings);
                Tenant t = tenantRepo.getTenantById(tenantId);
                t.setPlan(Plan.valueOf(planCode));
                tenantRepo.save(t);
                return session.getUrl();
            }

            if (provider == BillingProvider.PAYPAL) {

                String paypalPlanId = annual
                        ? plan.getPaypalPlanAnnualId()
                        : plan.getPaypalPlanMonthlyId();

                log.info("startCheckout PAYPAL: tenantId={} planCode={} billingCycle={} annual={} paypalPlanId={}",
                        tenantId, planCode, billingCycle, annual, paypalPlanId);

                if (paypalPlanId == null || paypalPlanId.isBlank()) {
                    throw new IllegalStateException("PayPal planId non configurato per planCode=" + planCode
                            + " billingCycle=" + billingCycle);
                }

                String success = "https://app.vercert.org/billing/paypal/success";
                String cancel  = "https://app.vercert.org/billing/paypal/cancel";

                String redirect = paypalGateway.createSubscription(
                        tenantId,
                        planCode,
                        billingCycle,
                        paypalPlanId,
                        success,
                        cancel
                );

                log.info("startCheckout PAYPAL: redirectUrl={}", redirect);

                // Aggiorno settings
                settings.setPlanCode(planCode);
                settings.setBillingCycle(billingCycle);
                settings.setProvider(provider.name());
                settings.setStatusEnum(PlanStatus.TRIALING);
                tenantSettingsRepo.save(settings);

                Tenant t = tenantRepo.getTenantById(tenantId);
                t.setPlan(Plan.valueOf(planCode));
                tenantRepo.save(t);

                return redirect;
            }

            throw new IllegalArgumentException("Provider di billing non supportato: " + provider);

        } catch (Exception e) {
            log.error("Errore nella creazione della sessione di pagamento: tenantId={} provider={} planCode={} billingCycle={}",
                    tenantId, provider, planCode, billingCycle, e);
            throw new IllegalStateException("Errore nella creazione della sessione di pagamento: " + e.getMessage(), e);
        }
    }
    /**
     * Chiamato da un webhook Stripe/PayPal quando il pagamento è confermato.
     * Qui attivi effettivamente il piano.
     */
    @Transactional
    public void activateSubscription(Long tenantId,
                                     String planCode,
                                     String billingCycle,
                                     BillingProvider provider,
                                     String providerSubscriptionId,
                                     String lastInvoiceId) {

        PlanDefinition plan = planDefinitionRepo.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Piano non trovato: " + planCode));

        TenantSettings s = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        Instant now = Instant.now();
        ZonedDateTime zdt = now.atZone(ZONE);
        s.setPlanCode(planCode);
        s.setBillingCycle(billingCycle);
        s.setProvider(provider.name());
        s.setSubscriptionId(providerSubscriptionId);
        s.setLastInvoiceId(lastInvoiceId);
        s.setCertsPerMonth(plan.getCertsPerMonth());
        s.setApiCallPerMonth(plan.getApiCallsPerMonth());
        s.setStorageMb(BigDecimal.valueOf(plan.getStorageMb()));
        s.setCurrentPeriodStart(zdt.toInstant());
        s.setCurrentPeriodEnd(calculatePeriodEnd(now, billingCycle));
        s.setStatusEnum(PlanStatus.ACTIVE);
        tenantSettingsRepo.save(s);
        Tenant t = tenantRepo.getTenantById(tenantId);
        t.setPlan(Plan.valueOf(planCode));
        tenantRepo.save(t);
    }

    /**
     * Chiamato da webhook quando Stripe/PayPal segnala problemi di pagamento.
     */
    @Transactional
    public void markPastDue(Long tenantId) {
        TenantSettings s = tenantSettingsRepo.findById(tenantId).orElseThrow();
        s.setStatusEnum(PlanStatus.PAST_DUE);
        tenantSettingsRepo.save(s);
    }

    @Transactional
    public void cancelSubscription(Long tenantId) {
        TenantSettings s = tenantSettingsRepo.findById(tenantId).orElseThrow();
        s.setStatusEnum(PlanStatus.CANCELED);
        tenantSettingsRepo.save(s);
    }

    // ====== PRIVATI ======

    private String resolvePriceId(PlanDefinition plan,
                                  String billingCycle,
                                  BillingProvider provider) {

        boolean annual = "ANNUAL".equalsIgnoreCase(billingCycle);

        switch (provider) {
            case STRIPE:
                String s = annual ? plan.getStripePriceAnnualId() : plan.getStripePriceMonthlyId();
                return s;
            case PAYPAL:
                String p = annual ? plan.getPaypalPlanAnnualId() : plan.getPaypalPlanMonthlyId();
                return p;
        };
        return null;
    }

    private Instant calculatePeriodEnd(Instant start, String billingCycle) {
        if ("ANNUAL".equalsIgnoreCase(billingCycle)) {
            return start.atZone(ZONE).plus(1, ChronoUnit.YEARS).toInstant();
        }
        // default: monthly
        return start.atZone(ZONE).plus(1, ChronoUnit.MONTHS).toInstant();
    }

    @Transactional
    public void renewStripeSubscription(Long tenantId,
                                        String planCode,
                                        String billingCycle,
                                        String providerSubscriptionId,
                                        String invoiceId,
                                        Instant currentPeriodStart,
                                        Instant currentPeriodEnd) {

        TenantSettings s = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        // Non tocco i limiti, perché sono già presi da PlanDefinition
        s.setPlanCode(planCode);
        s.setBillingCycle(billingCycle);
        s.setProvider("STRIPE");
        s.setSubscriptionId(providerSubscriptionId);
        s.setLastInvoiceId(invoiceId);
        s.setCurrentPeriodStart(currentPeriodStart);
        s.setCurrentPeriodEnd(currentPeriodEnd);
        s.setStatusEnum(PlanStatus.ACTIVE);
        tenantSettingsRepo.save(s);
        planUsageResetService.resetUsageForNewPeriod(tenantId);
    }

    @Transactional
    public void renewPaypalSubscription(Long tenantId,
                                        String planCode,
                                        String billingCycle,
                                        String providerSubscriptionId,
                                        String transactionId,
                                        Instant currentPeriodStart,
                                        Instant currentPeriodEnd) {

        TenantSettings s = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant Settings mancanti per " + tenantId));

        s.setPlanCode(planCode);
        s.setBillingCycle(billingCycle);
        s.setProvider("PAYPAL");
        s.setSubscriptionId(providerSubscriptionId);
        s.setLastInvoiceId(transactionId);
        s.setCurrentPeriodStart(currentPeriodStart);
        s.setCurrentPeriodEnd(currentPeriodEnd);
        s.setStatusEnum(PlanStatus.ACTIVE);
        tenantSettingsRepo.save(s);
        planUsageResetService.resetUsageForNewPeriod(tenantId);
    }

    @Transactional
    public void activateSubscriptionForTenant(Long tenantId, String subId) {

        TenantSettings ts = tenantSettingsRepo.findByTenantId(tenantId).orElseThrow();
        if (subId == null || subId.isBlank() || !subId.startsWith("I-")) {
            throw new IllegalStateException("Missing PayPal subscription_id");
        }
        this.activateSubscription(
                tenantId,
                ts.getPlanCode(),
                ts.getBillingCycle(),
                BillingProvider.PAYPAL,
                subId,
                null
        );
    }
}