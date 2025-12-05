package com.example.vericert;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.service.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

// src/test/java/com/example/vericert/service/BillingServiceTest.java

import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.domain.Tenant;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.enumerazioni.Plan;
import com.example.vericert.enumerazioni.PlanStatus;
import com.example.vericert.repo.PlanDefinitionRepository;
import com.example.vericert.repo.TenantRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class BillingServiceTest {

    private TenantSettingsRepository tenantSettingsRepo;
    private PlanDefinitionRepository planDefinitionRepo;
    private StripeGateway stripeGateway;
    private TenantRepository tenantRepo;
    private PaypalGateway paypalGateway;
    private BillingService billingService;
    private PlanUsageResetService planUsageResetService;
    @Autowired
    PaymentsProps paymentsProps;


    @BeforeEach
    void setUp() {
        tenantSettingsRepo = mock(TenantSettingsRepository.class);
        planDefinitionRepo = mock(PlanDefinitionRepository.class);
        stripeGateway = mock(StripeGateway.class);
        tenantRepo = mock(TenantRepository.class);
        paypalGateway= mock(PaypalGateway.class);
        planUsageResetService = mock(PlanUsageResetService.class);



        billingService = new BillingService(
                tenantSettingsRepo,
                planDefinitionRepo,
                stripeGateway,
                tenantRepo,
                paypalGateway,
                planUsageResetService
        );

    }

    @Test
    void startCheckout_stripeMonthly_updatesSettingsAndTenant() throws StripeException {
        Long tenantId = 1L;
        String planCode = "PRO";
        Stripe.apiKey = paymentsProps.getStripe().getSecretKey();
        PlanDefinition def = new PlanDefinition();
        def.setCode(planCode);
        def.setStripePriceMonthlyId("price_1SXPMXIX50JfMIoY8IequQDT");
        when(planDefinitionRepo.findByCode(planCode)).thenReturn(Optional.of(def));

        TenantSettings settings = new TenantSettings();
        settings.setTenantId(tenantId);
        when(tenantSettingsRepo.findById(tenantId)).thenReturn(Optional.of(settings));

        Tenant tenant = new Tenant();
        when(tenantRepo.getTenantById(tenantId)).thenReturn(tenant);

        var priceId = resolvePriceId(def, "MONTHLY", BillingProvider.STRIPE);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("http://localhost:8080/billing/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("http://localhost:8080/billing/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("tenant_id", tenantId.toString())
                                .putMetadata("plan_code", planCode)
                                .putMetadata("billing_cycle", "MONTHLY")
                                .build()
                )
                .build();

        when(stripeGateway.createCheckoutSession(eq(tenantId),
                eq("price_1SXPMXIX50JfMIoY8IequQDT"), eq(planCode), eq("MONTHLY")))
                .thenReturn(Session.create(params));

        String redirect = billingService.startCheckout(
                tenantId, planCode, "MONTHLY", BillingProvider.STRIPE);

        ArgumentCaptor<TenantSettings> settingsCaptor = ArgumentCaptor.forClass(TenantSettings.class);
        verify(tenantSettingsRepo).save(settingsCaptor.capture());
        TenantSettings saved = settingsCaptor.getValue();

        assertThat(saved.getPlanCode()).isEqualTo(planCode);
        assertThat(saved.getBillingCycle()).isEqualTo("MONTHLY");
        assertThat(saved.getProvider()).isEqualTo("STRIPE");
        assertThat(saved.getStatusEnum()).isEqualTo(PlanStatus.TRIALING);

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepo).save(tenantCaptor.capture());
        Tenant savedTenant = tenantCaptor.getValue();
        assertThat(savedTenant.getPlan()).isEqualTo(Plan.PRO);
    }

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

    @Test
    void startCheckout_throwsIfPlanNotFound() {
        when(planDefinitionRepo.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> billingService.startCheckout(1L, "UNKNOWN", "MONTHLY", BillingProvider.STRIPE)
        );
    }
    @Test
    void activateSubscription_setsPlanLimitsAndStatusActive() {
        Long tenantId = 1L;
        String planCode = "PRO";

        PlanDefinition def = new PlanDefinition();
        def.setCode(planCode);
        def.setCertsPerMonth(100);
        def.setApiCallsPerMonth(50000);
        def.setStorageMb(5000L);
        when(planDefinitionRepo.findByCode(planCode)).thenReturn(Optional.of(def));

        TenantSettings settings = new TenantSettings();
        settings.setTenantId(tenantId);
        when(tenantSettingsRepo.findById(tenantId)).thenReturn(Optional.of(settings));

        billingService.activateSubscription(
                tenantId,
                planCode,
                "MONTHLY",
                BillingProvider.STRIPE,
                "sub_123",
                "inv_123"
        );

        verify(tenantSettingsRepo).save(settings);
        assertThat(settings.getPlanCode()).isEqualTo(planCode);
        assertThat(settings.getBillingCycle()).isEqualTo("MONTHLY");
        assertThat(settings.getProvider()).isEqualTo("STRIPE");
        assertThat(settings.getSubscriptionId()).isEqualTo("sub_123");
        assertThat(settings.getLastInvoiceId()).isEqualTo("inv_123");
        assertThat(settings.getCertsPerMonth()).isEqualTo(100);
        assertThat(settings.getApiCallPerMonth()).isEqualTo(50000);
        assertThat(settings.getStorageMb()).isEqualTo(5000L);
        assertThat(settings.getStatusEnum()).isEqualTo(PlanStatus.ACTIVE);
    }

}

