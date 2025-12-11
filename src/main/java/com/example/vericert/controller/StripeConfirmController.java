package com.example.vericert.controller;

import com.example.vericert.component.PaymentsProps;
import com.example.vericert.repo.PaymentRepository;
import com.example.vericert.service.AdminPlanDefinitionsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/payments/stripe")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StripeConfirmController {
    private final PaymentRepository payRepo;
    private final PaymentsProps props;
    private final AdminPlanDefinitionsService service;




    public StripeConfirmController(PaymentRepository payRepo,
                                   PaymentsProps props,
                                   AdminPlanDefinitionsService service) {


        this.payRepo = payRepo; this.props = props;
        com.stripe.Stripe.apiKey = props.getStripe().getSecretKey();
        this.service = service;
    }

    @PostMapping("/confirm")
    public void confirm(@RequestParam String sessionId) throws Exception {
        var session = com.stripe.model.checkout.Session.retrieve(sessionId);
        if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
            payRepo.findByCheckoutSessionId(sessionId).ifPresent(p -> {
                p.setStatus("SUCCEEDED");
                if (p.getProviderIntentId() == null && session.getPaymentIntent() != null) {
                    p.setProviderIntentId(session.getPaymentIntent());
                }
                p.setUpdatedAt(Instant.now());
                payRepo.save(p);
                String tenantId = session.getMetadata().get("tenantId");
                String planCode = session.getMetadata().get("planCode");
                String cycle = session.getMetadata().get("billingCycle");
                String planDefId= session.getMetadata().get("planDefId");
                // Persisti attivazione
                service.activatePlan(Long.valueOf(tenantId), planCode, cycle, session.getId(), "STRIPE");
            });
        }
    }
}
