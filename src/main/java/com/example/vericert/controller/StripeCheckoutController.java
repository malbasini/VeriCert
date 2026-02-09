package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.domain.Payment;
import com.example.vericert.repo.PaymentRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeCheckoutController {

private final PaymentRepository payRepo;
private final PaymentsProps props;

public StripeCheckoutController(PaymentsProps props, PaymentRepository payRepo) {
    this.props = props;
    this.payRepo = payRepo;
    com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
}


@PostMapping("/checkout")
public Map<String,Object> createCheckout(@RequestParam Long tenantId,
                                         @RequestParam long amountMinor,
                                         @RequestParam(required=false) Long certificateId,
                                         @RequestParam(defaultValue="EUR") String currency,
                                         @RequestParam(defaultValue="Certificato VeriCert") String description,
                                         @RequestParam String plan
) throws Exception {
    var lineItem = new HashMap<String,Object>();
    lineItem.put("price_data", Map.of(
            "currency", currency.toLowerCase(),
            "unit_amount", amountMinor,
            "product_data", Map.of("name", description)
    ));
    lineItem.put("quantity", 1);


    Map<String,String> metadata = new HashMap<>();
    metadata.put("tenantId", String.valueOf(tenantId));
    if (certificateId != null) metadata.put("certificateId", String.valueOf(certificateId));
    var params = new HashMap<String,Object>();
    params.put("mode", "payment");
    params.put("line_items", List.of(lineItem));
    params.put("success_url", props.getSuccessUrl());
    params.put("cancel_url", props.getCancelUrl());
    params.put("metadata", metadata);
    params.put("payment_intent_data", Map.of("metadata", metadata));
    params.put("allow_promotion_codes", true);
    params.put("automatic_tax", Map.of("enabled", false)); // opzionale


    // opzionale: idempotenza lato Stripe
    var idem = UUID.randomUUID().toString();
    var reqOpts = com.stripe.net.RequestOptions.builder().setIdempotencyKey(idem).build();


    var session = com.stripe.model.checkout.Session.create(params, reqOpts);

    // salva PENDING
    Payment p = new Payment();
    p.setTenantId(tenantId);
    p.setCertificateId(certificateId);
    p.setProvider("STRIPE");
    p.setCheckoutSessionId(session.getId());
    p.setProviderIntentId(session.getPaymentIntent()); // può essere null in questo momento
    p.setStatus("PENDING");
    p.setAmountMinor(amountMinor);
    p.setCurrency(currency.toUpperCase());
    p.setDescription(description);
    p.setIdempotencyKey(idem);
    p.setCreatedAt(Instant.now());
    p.setUpdatedAt(Instant.now());
    payRepo.save(p);


    return Map.of(
            "checkoutSessionId", session.getId(),
            "url", session.getUrl(),
            "publishableKey", props.getStripe().getPublishableKey()
    );
  }

}