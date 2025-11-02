package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.repo.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeCheckoutRedirectController {

    private final PaymentsProps props;
    private final PaymentRepository payRepo;

    public StripeCheckoutRedirectController(PaymentsProps props, PaymentRepository payRepo) {
        this.props = props; this.payRepo = payRepo;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
    }

    @PostMapping("/checkout-redirect")
    public ResponseEntity<Void> createAndRedirect(@RequestParam Long tenantId,
                                                  @RequestParam long amountMinor,
                                                  @RequestParam(required=false) Long certificateId,
                                                  @RequestParam(defaultValue="EUR") String currency,
                                                  @RequestParam(defaultValue="Certificato VeriCert") String description
    ) throws Exception {
        var lineItem = new java.util.HashMap<String,Object>();
        lineItem.put("price_data", java.util.Map.of(
                "currency", currency.toLowerCase(),
                "unit_amount", amountMinor,
                "product_data", java.util.Map.of("name", description)
        ));
        lineItem.put("quantity", 1);

        var metadata = new java.util.HashMap<String,String>();
        metadata.put("tenantId", String.valueOf(tenantId));
        if (certificateId != null) metadata.put("certificateId", String.valueOf(certificateId));

        var params = new java.util.HashMap<String,Object>();
        params.put("mode", "payment");
        params.put("line_items", java.util.List.of(lineItem));
        params.put("success_url", props.getSuccessUrl());
        params.put("cancel_url",  props.getCancelUrl());
        params.put("metadata", metadata);
        params.put("payment_intent_data", java.util.Map.of("metadata", metadata));
        // params.put("automatic_tax", java.util.Map.of("enabled", true)); // opzionale

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
}
