package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.repo.PaymentRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripeConfirmController {
    private final PaymentRepository payRepo;
    private final PaymentsProps props;
    public StripeConfirmController(PaymentRepository payRepo, PaymentsProps props) {


        this.payRepo = payRepo; this.props = props;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
    }

    @PostMapping("/confirm")
    public void confirm(@RequestParam String sessionId) throws Exception {
        var s = com.stripe.model.checkout.Session.retrieve(sessionId);
        if ("paid".equalsIgnoreCase(s.getPaymentStatus())) {
            payRepo.findByCheckoutSessionId(sessionId).ifPresent(p -> {
                p.setStatus("SUCCEEDED");
                if (p.getProviderIntentId() == null && s.getPaymentIntent() != null) {
                    p.setProviderIntentId(s.getPaymentIntent());
                }
                p.setUpdatedAt(Instant.now());
                payRepo.save(p);
            });
        }
    }
}
