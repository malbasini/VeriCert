package com.example.vericert.repo;

import com.example.vericert.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByCheckoutSessionId(String sessionId);
    Optional<Payment> findByProviderIntentId(String providerIntentId);
    boolean existsByIdempotencyKey(String idem);
}