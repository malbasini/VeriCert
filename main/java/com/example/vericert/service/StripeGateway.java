package com.example.vericert.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeGateway {

    public StripeGateway(@Value("${stripe.secret-key}") String apiKey) {
        Stripe.apiKey = apiKey; // inizializzi l’SDK Stripe una volta
    }

    public String createCheckoutSession(Long tenantId,
                                        String priceId,
                                        String planCode,
                                        String billingCycle) throws Exception {

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setSuccessUrl("http://localhost/billing/success")
                        .setCancelUrl("http://localhost/billing/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPrice(priceId) // PREZZO del piano scelto
                                        .build()
                        )
                        // metadata per il webhook
                        .putMetadata("tenant_id", tenantId.toString())
                        .putMetadata("plan_code", planCode)
                        .putMetadata("billing_cycle", billingCycle)
                        .build();

        Session session = Session.create(params);

        // URL su cui fai redirect dal controller Billing
        return session.getUrl();
    }
}
