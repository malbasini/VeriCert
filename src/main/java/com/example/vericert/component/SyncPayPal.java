package com.example.vericert.component;

import com.example.vericert.domain.Payment;
import com.example.vericert.domain.TenantSettings;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.repo.TenantSettingsRepository;
import com.example.vericert.service.BillingService;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

public class SyncPayPal {

    private final PaypalClient paypalClient;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final PaymentRepository paymentRepo;
    private final BillingService billingService;

    public SyncPayPal(PaypalClient paypalClient,
                      TenantSettingsRepository tenantSettingsRepo,
                      PaymentRepository paymentRepo,
                      BillingService billingService) {
        this.paypalClient = paypalClient;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.paymentRepo = paymentRepo;
        this.billingService = billingService;
    }
    @Scheduled(fixedDelayString = "PT10M")
    @Transactional
    public void SyncPaypalRenewals() {

        for (TenantSettings ts : tenantSettingsRepo.findTenantsWithPaypalSubscription()) {

            String subId = ts.getSubscriptionId();
            if (subId == null || !subId.startsWith("I-")) continue;

            Map<String, Object> s = paypalClient.get("/v1/billing/subscriptions/" + subId, Map.class);

            Map<String, Object> billingInfo = (Map<String, Object>) s.get("billing_info");
            if (billingInfo == null) continue;

            Map<String, Object> lastPayment = (Map<String, Object>) billingInfo.get("last_payment");
            if (lastPayment == null) continue;

            String lastPaymentTime = String.valueOf(lastPayment.get("time")); // ISO
            if (lastPaymentTime == null || "null".equals(lastPaymentTime)) continue;

            Map<String, Object> amount = (Map<String, Object>) lastPayment.get("amount");
            String valueStr = amount != null ? String.valueOf(amount.get("value")) : null;

            String idem = "PAYPAL:RENEW:" + subId + ":" + lastPaymentTime;

            Payment p = new Payment();
            p.setTenantId(ts.getTenantId());
            p.setProvider("PAYPAL");
            p.setProviderIntentId(subId);
            p.setStatus("SUCCEEDED");
            p.setCurrency("EUR");
            p.setAmountMinor(parseEuroToCents(valueStr)); // 0.61 -> 61
            p.setIdempotencyKey(idem);
            p.setDescription(ts.getPlanCode() + " " + ts.getBillingCycle() + " - Rinnovo");
            try {
                paymentRepo.saveAndFlush(p);
            } catch (DataIntegrityViolationException dup) {
                continue; // rinnovo già gestito
            }

            Instant periodStart = ts.getCurrentPeriodEnd();
            Instant periodEnd = computeNextPeriodEnd(periodStart, ts.getBillingCycle());

            billingService.renewPaypalSubscription(
                    ts.getTenantId(),
                    ts.getPlanCode(),
                    ts.getBillingCycle(),
                    subId,     // providerSubscriptionId
                    idem,      // ref (o transactionId se ce l’hai)
                    periodStart,
                    periodEnd
            );
        }
    }
    private long parseEuroToCents(String value) {
        if (value == null) return 0L;
        BigDecimal bd = new BigDecimal(value); // "0.61"
        return bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private Instant computeNextPeriodEnd(Instant start, String cycle) {
        ZonedDateTime z = start.atZone(ZoneId.of("Europe/Rome"));
        return switch (String.valueOf(cycle).toUpperCase()) {
            case "YEARLY", "ANNUAL", "ANNUALLY" -> z.plusYears(1).toInstant();
            default -> z.plusMonths(1).toInstant(); // MONTHLY
        };
    }
}
