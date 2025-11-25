package com.example.vericert.controller;

import com.example.vericert.domain.TenantSettings;
import com.example.vericert.enumerazioni.BillingProvider;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.BillingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/webhooks")
public class PaypalWebhookController {

    private final BillingService billingService;
    private final ObjectMapper objectMapper;
    private final TenantSettingsRepository tenantSettingsRepo;
    public PaypalWebhookController(BillingService billingService,
                                   ObjectMapper objectMapper,
                                   TenantSettingsRepository tenantSettingsRepo) {
        this.billingService = billingService;
        this.objectMapper = objectMapper;
        this.tenantSettingsRepo = tenantSettingsRepo;
    }

    @PostMapping("/paypal")
    public ResponseEntity<String> handlePaypalWebhook(@RequestBody String payload,
                                                      @RequestHeader("Paypal-Transmission-Sig") String signature) {

        // TODO: verifica firma webhook PayPal con le loro SDK / API

        try {
            JsonNode root;
            root = objectMapper.readTree(payload);
            String eventType = root.path("event_type").asText();

            // 1) attivazione iniziale
            if ("BILLING.SUBSCRIPTION.ACTIVATED".equals(eventType)) {
                JsonNode resource = root.path("resource");
                String subscriptionId = resource.path("id").asText();
                String tenantIdStr = resource.path("custom_id").asText(); // se lo metti qui
                String planCode = resource.path("plan_id").asText(); // o mappi plan_id -> plan_code
                String billingCycle = "MONTHLY"; // o deduci dalle settings PayPal

                Long tenantId = Long.valueOf(tenantIdStr);

                // qui puoi chiamare activateSubscription(...) come per Stripe
                billingService.activateSubscription(
                        tenantId,
                        planCode,
                        billingCycle,
                        BillingProvider.PAYPAL,
                        subscriptionId,
                        null // invoice id se lo hai
                );
            }

            // 2) pagamento ricorrente: rinnovo
            if ("PAYMENT.SALE.COMPLETED".equals(eventType)) {
                JsonNode resource = root.path("resource");

                // in questo evento trovi spesso billing_agreement_id = subscription id
                String subscriptionId = resource.path("billing_agreement_id").asText();

                // Qui devi:
                // - risalire al tenant (o via custom field memorizzata prima,
                //   o cercando TenantSettings per subscriptionId)
                TenantSettings s = tenantSettingsRepo.findBySubscriptionId(subscriptionId)
                        .orElse(null);
                if (s != null) {
                    Long tenantId = s.getTenantId();
                    String planCode = s.getPlanCode();
                    String billingCycle = s.getBillingCycle();

                    // PayPal restituisce spesso next_billing_time nella subscription,
                    // ma NON in PAYMENT.SALE.COMPLETED.
                    // Puoi:
                    // - o fare una GET alla Subscriptions API per leggere next_billing_time
                    // - oppure calcolare tu current_period_start/end aggiungendo 1 mese/1 anno
                    Instant now = Instant.now();
                    Instant newEnd = calculateNextPeriodEnd(now, billingCycle);

                    billingService.renewPaypalSubscription(
                            tenantId,
                            planCode,
                            billingCycle,
                            subscriptionId,
                            resource.path("id").asText(), // transaction id,
                            now,
                            newEnd
                    );
                }
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }

        return ResponseEntity.ok("");
    }

    private Instant calculateNextPeriodEnd(Instant start, String billingCycle) {
        if ("ANNUAL".equalsIgnoreCase(billingCycle)) {
            return start.plus(365, ChronoUnit.DAYS);
        }
        return start.plus(30, ChronoUnit.DAYS); // semplice approssimazione
    }
}
