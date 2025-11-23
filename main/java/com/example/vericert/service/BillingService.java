package com.example.vericert.service;

import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class BillingService {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanDefinitionRepository planDefinitionRepo;
    private final StripeGateway stripeGateway;

    public BillingService(TenantSettingsRepository tenantSettingsRepo,
                          PlanDefinitionRepository planDefinitionRepo,
                          StripeGateway stripeGateway) {
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.planDefinitionRepo = planDefinitionRepo;
        this.stripeGateway = stripeGateway;
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

        PlanDefinition plan = planDefinitionRepo.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Piano non trovato: " + planCode));

        TenantSettings settings = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        String priceId = resolvePriceId(plan, billingCycle, provider);
        String redirectUrl = "";
        // TODO: qui agganci Stripe/PayPal veri.
        try {
            if (provider == BillingProvider.STRIPE) {
                redirectUrl = stripeGateway.createCheckoutSession(
                        tenantId, priceId, planCode, billingCycle
                );
            } else {
                // redirectUrl = paypalGateway.createCheckout(...);
                throw new UnsupportedOperationException("PayPal da implementare");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore nella creazione della sessione di pagamento", e);
        }
        String checkoutSessionId = "dummy_session_" + System.currentTimeMillis();
        redirectUrl = redirectUrl + checkoutSessionId;

        // Salvi nella settings l'ultimo checkout in corso
        settings.setPlanCode(planCode);
        settings.setBillingCycle(billingCycle);
        settings.setProvider(provider.name());
        settings.setCheckoutSessionId(checkoutSessionId);
        settings.setStatusEnum(PlanStatus.TRIALING); // o "PENDING"
        tenantSettingsRepo.save(settings);

        return redirectUrl;
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

        s.setPlanCode(planCode);
        s.setBillingCycle(billingCycle);
        s.setProvider(provider.name());
        s.setSubscriptionId(providerSubscriptionId);
        s.setLastInvoiceId(lastInvoiceId);

        s.setCertsPerMonth(plan.getCertsPerMonth());
        s.setApiCallPerMonth(plan.getApiCallsPerMonth());
        s.setStorageMb(plan.getStorageMb());

        s.setCurrentPeriodStart(now);
        s.setCurrentPeriodEnd(calculatePeriodEnd(now, billingCycle));

        s.setStatusEnum(PlanStatus.ACTIVE);

        tenantSettingsRepo.save(s);
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

        return switch (provider) {
            case STRIPE -> annual ? plan.getStripePriceAnnualId()
                    : plan.getStripePriceMonthlyId();
            case PAYPAL -> annual ? plan.getPaypalPlanAnnualId()
                    : plan.getPaypalPlanMonthlyId();
        };
    }

    private Instant calculatePeriodEnd(Instant start, String billingCycle) {
        if ("ANNUAL".equalsIgnoreCase(billingCycle)) {
            return start.atZone(ZoneOffset.UTC).plus(1, ChronoUnit.YEARS).toInstant();
        }
        // default: monthly
        return start.atZone(ZoneOffset.UTC).plus(1, ChronoUnit.MONTHS).toInstant();
    }
}
