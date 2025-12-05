package com.example.vericert.service;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class BillingService {

    private final TenantSettingsRepository tenantSettingsRepo;
    private final PlanDefinitionRepository planDefinitionRepo;
    private final StripeGateway stripeGateway;
    private final TenantRepository tenantRepo;
    private final PaypalGateway  paypalGateway;
    private final PlanUsageResetService planUsageResetService;

    public BillingService(TenantSettingsRepository tenantSettingsRepo,
                          PlanDefinitionRepository planDefinitionRepo,
                          StripeGateway stripeGateway,
                          TenantRepository tenantRepo,
                          PaypalGateway paypalGateway,
                          PlanUsageResetService planUsageResetService) {

        this.tenantSettingsRepo = tenantSettingsRepo;
        this.planDefinitionRepo = planDefinitionRepo;
        this.stripeGateway = stripeGateway;
        this.tenantRepo = tenantRepo;
        this.paypalGateway = paypalGateway;
        this.planUsageResetService = planUsageResetService;
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
        String priceId = resolvePriceId(plan, billingCycle, provider);
        Session session = null;
        String redirect = "";
        try {
            if (provider == BillingProvider.STRIPE) {
                // ⬇⬇ stripeGateway deve restituire UNA Session di Stripe
                session = stripeGateway.createCheckoutSession(tenantId, priceId, planCode, billingCycle);
            } else {
                String paypalPlanId = annual
                        ? plan.getPaypalPlanAnnualId()
                        : plan.getPaypalPlanMonthlyId();

                String success = "http://localhost:8080/billing/paypal/success";
                String cancel =  "http://localhost:8080/billing/paypal/cancel";

                redirect = paypalGateway.createSubscription(
                        tenantId,
                        planCode,
                        billingCycle,
                        paypalPlanId,
                        success,
                        cancel
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore nella creazione della sessione di pagamento", e);
        }
        if(provider == BillingProvider.STRIPE) {

            // URL ESATTA DI STRIPE per il redirect
            String redirectUrl = session.getUrl();
            // ID ESATTO DELLA SESSIONE STRIPE (tipo cs_test_...)
            String checkoutSessionId = session.getId();

            // Salvo nel DB informazioni corrette
            settings.setPlanCode(planCode);
            settings.setBillingCycle(billingCycle);
            settings.setProvider(provider.name());
            settings.setCheckoutSessionId(checkoutSessionId);
            settings.setStatusEnum(PlanStatus.TRIALING); // o PENDING
            tenantSettingsRepo.save(settings);

            Tenant t = tenantRepo.getTenantById(tenantId);
            t.setPlan(Plan.valueOf(planCode));
            tenantRepo.save(t);

            return redirectUrl;  // 👈 NIENTE concatenazioni, è già pronta
        }
        if(provider == BillingProvider.PAYPAL) {
            
            // Salvo nel DB informazioni corrette
            settings.setPlanCode(planCode);
            settings.setBillingCycle(billingCycle);
            settings.setProvider(provider.name());
            settings.setStatusEnum(PlanStatus.TRIALING); // o PENDING
            tenantSettingsRepo.save(settings);
            Tenant t = tenantRepo.getTenantById(tenantId);
            t.setPlan(Plan.valueOf(planCode));
            tenantRepo.save(t);

            return redirect;  // 👈 NIENTE concatenazioni, è già pronta
        }
        return "";
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
            return start.atZone(ZoneOffset.UTC).plus(1, ChronoUnit.YEARS).toInstant();
        }
        // default: monthly
        return start.atZone(ZoneOffset.UTC).plus(1, ChronoUnit.MONTHS).toInstant();
    }

    @Transactional
    public void renewStripeSubscription(Long tenantId,
                                        String planCode,
                                        String billingCycle,
                                        String providerSubscriptionId,
                                        String invoiceId,
                                        long currentPeriodStartEpoch,
                                        long currentPeriodEndEpoch) {

        TenantSettings s = tenantSettingsRepo.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

        // Non tocco i limiti, perché sono già presi da PlanDefinition
        s.setPlanCode(planCode);
        s.setBillingCycle(billingCycle);
        s.setProvider("STRIPE");
        s.setSubscriptionId(providerSubscriptionId);
        s.setLastInvoiceId(invoiceId);

        Instant start = Instant.ofEpochSecond(currentPeriodStartEpoch);
        Instant end = Instant.ofEpochSecond(currentPeriodEndEpoch);

        s.setCurrentPeriodStart(start);
        s.setCurrentPeriodEnd(end);

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
                .orElseThrow(() -> new IllegalStateException("TenantSettings mancanti per " + tenantId));

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
    private Long calculateAmount(Long amountMinor, String plan) {
        BigDecimal vat = BigDecimal.valueOf(0);
        BigDecimal discount = BigDecimal.valueOf(0L);
        switch (plan){
            case "MONTHLY":
                //Calcolo l'iva
                vat = BigDecimal.valueOf((amountMinor * 22) / 100);
                vat = BigDecimal.valueOf(Math.round(vat.doubleValue()));
                amountMinor = Math.round(amountMinor.doubleValue() + vat.doubleValue());
                break;
            case "ANNUAL":
                //Calcolo l'iva
                vat = BigDecimal.valueOf((amountMinor * 22) / 100);
                vat = BigDecimal.valueOf(Math.round(vat.doubleValue()));
                amountMinor = (Math.round(amountMinor.doubleValue() + vat.doubleValue())) * 12;
                break;
            default:
                throw new IllegalArgumentException("Plan non supportato");

        }
        return amountMinor;
    }






















}
