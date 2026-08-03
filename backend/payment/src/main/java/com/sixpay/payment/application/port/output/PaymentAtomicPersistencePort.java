package com.sixpay.payment.application.port.output;

import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;

import java.time.Instant;
import java.util.List;

/**
 * Persists one changed Payment together with its audit and outbox records.
 */
public interface PaymentAtomicPersistencePort {

    Payment persist(
            Payment payment,
            List<PaymentDomainEvent> events,
            Instant stagedAt
    );
}
