package com.example.vericert.enumerazioni;

public enum PlanStatus {
    TRIALING,   // prova gratuita
    ACTIVE,     // tutto ok
    PAST_DUE,   // pagamento in ritardo (Stripe past_due / PayPal problemi)
    CANCELED,   // abbonamento cancellato dal cliente o da te
    EXPIRED,
    PENDING,
    SUCCEDED// periodo scaduto e nessun rinnovo
}