package com.example.vericert.service;

import com.example.vericert.component.PaymentsProps;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeGateway {

    private final PaymentsProps props;

    public StripeGateway(PaymentsProps props) {
        this.props = props;
        Stripe.apiKey = props.getStripe().getSecretKey();; // inizializzi l’SDK Stripe una volta
    }

    public Session createCheckoutSession(Long tenantId,
                                         String priceId,
                                         String planCode,
                                         String billingCycle) throws StripeException {


        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl("https://app.vercert.org/billing/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://app.vercert.org/billing/cancel")
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
                                .putMetadata("billing_cycle", billingCycle)
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session;
    }

}
