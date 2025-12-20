package com.example.vericert.service;

import com.stripe.model.Subscription;
import org.springframework.stereotype.Service;

@Service
public class StripeSubscriptionService {

    public Subscription findById(String subscriptionId) throws Exception {
        return Subscription.retrieve(subscriptionId);
    }
}
