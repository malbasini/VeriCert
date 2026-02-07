package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.domain.PlanDefinition;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments/stripe")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StripeCheckoutRedirectController {

    private final PaymentsProps props;
    private final PaymentRepository payRepo;
    private final AdminPlanDefinitionsService service;

    public StripeCheckoutRedirectController(PaymentsProps props,
                                            PaymentRepository payRepo,
                                            AdminPlanDefinitionsService service) {
        this.props = props; this.payRepo = payRepo;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.service = service;
    }

    @PostMapping("/checkout-redirect")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> createAndRedirect(@RequestParam Long tenantId,
                                                  @RequestParam Long amountMinor,
                                                  @RequestParam(required=false) Long certificateId,
                                                  @RequestParam(defaultValue="EUR") String currency,
                                                  @RequestParam(defaultValue="Certificato VeriCert") String description,
                                                  @RequestParam String planCode,
                                                  @RequestParam String billingCycle
    ) throws Exception {

        billingCycle = billingCycle.toUpperCase();
        PlanDefinition plan = service.getPlan(planCode);
        boolean annual = "ANNUAL".equalsIgnoreCase(billingCycle);
        String duration = annual ? "ANNUAL" : "MONTHLY";
        String desc = "Piano " + planCode + (annual ? " (Annuale -20%)" : " (Mensile)");
        description = desc;
        long amountdb = annual ? plan.getPriceAnnualCents() : plan.getPriceMonthlyCents();
        amountMinor = amountdb;
        //amountMinor è il prezzo in centesimi annuale o mensile.
        Long price = calculateAmount(amountMinor, duration);
        amountMinor = price;
        if(!annual)
            amountMinor +=1;
        var lineItem = new java.util.HashMap<String,Object>();
        lineItem.put("price_data", java.util.Map.of(
                "currency", currency.toLowerCase(),
                "unit_amount", amountMinor,
                "product_data", java.util.Map.of("name", description)
        ));
        lineItem.put("quantity", 1);
        var metadata = new java.util.HashMap<String,String>();
        metadata.put("tenantId", String.valueOf(tenantId));
        metadata.put("planCode", planCode);
        metadata.put("billingCycle", billingCycle);
        metadata.put("planDefId", String.valueOf(plan.getId()));
        if (certificateId != null) metadata.put("certificateId", String.valueOf(certificateId));
        var params = new java.util.HashMap<String,Object>();
        params.put("mode", "payment");
        params.put("line_items", java.util.List.of(lineItem));
        params.put("success_url", props.getSuccessUrl());
        params.put("cancel_url",  props.getCancelUrl());
        params.put("metadata", metadata);
        params.put("payment_intent_data", java.util.Map.of("metadata", metadata));
        params.put("automatic_tax", java.util.Map.of("enabled", false)); // opzionale

        // (opzionale) idempotenza
        var idem = java.util.UUID.randomUUID().toString();
        var reqOpts = com.stripe.net.RequestOptions.builder().setIdempotencyKey(idem).build();

        var session = com.stripe.model.checkout.Session.create(params, reqOpts);

        // salva PENDING
        var p = new Payment();
        p.setTenantId(tenantId);
        p.setCertificateId(certificateId);
        p.setProvider("STRIPE");
        p.setCheckoutSessionId(session.getId());
        p.setProviderIntentId(session.getPaymentIntent()); // può essere null ora
        p.setStatus("PENDING");
        p.setAmountMinor(amountMinor);
        p.setCurrency(currency.toUpperCase());
        p.setDescription(description);
        p.setIdempotencyKey(idem);
        p.setCreatedAt(java.time.Instant.now());
        p.setUpdatedAt(java.time.Instant.now());
        payRepo.save(p);

        // Redirect 303 alla pagina ospitata da Stripe
        return ResponseEntity.status(303)
                .header("Location", session.getUrl())
                .build();
    }
    // Calcola l'importo da pagare'
    private Long calculateAmount(Long amountMinor, String plan) {
        BigDecimal vat;
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
