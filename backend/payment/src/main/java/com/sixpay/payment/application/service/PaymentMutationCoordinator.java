package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.output.PaymentAtomicPersistencePort;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared orchestration mechanism for focused Payment workflow services.
 *
 * <p>The coordinator depends exclusively on application ports. Infrastructure
 * details such as JPA, audit and outbox persistence remain behind outbound
 * adapters.</p>
 */
@Component
public class PaymentMutationCoordinator {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentAtomicPersistencePort atomicPersistencePort;
    private final TimeProvider timeProvider;

    public PaymentMutationCoordinator(
            PaymentLookupPort paymentLookupPort,
            PaymentAtomicPersistencePort atomicPersistencePort,
            TimeProvider timeProvider
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.atomicPersistencePort = Objects.requireNonNull(
                atomicPersistencePort,
                "Payment atomic persistence port"
        );
        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
    }

    /**
     * Persists a newly created aggregate together with every domain event it
     * emitted during creation. A changed aggregate without events is rejected
     * because it could not be audited or published reliably.
     */
    public PaymentWorkflowResult persistNew(Payment payment) {
        Objects.requireNonNull(payment, "Payment");

        List<PaymentDomainEvent> events =
                requireEvents(payment);

        Instant stagedAt = safeStagedAt(
                events,
                timeProvider.now()
        );

        Payment persisted = atomicPersistencePort.persist(
                payment,
                events,
                stagedAt
        );

        return PaymentWorkflowResult.from(
                persisted,
                true
        );
    }

    public PaymentWorkflowResult mutate(
            PaymentId paymentId,
            Consumer<Payment> mutation
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(mutation, "Payment mutation");

        Payment payment = paymentLookupPort
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );

        long previousVersion =
                payment.businessVersion();

        mutation.accept(payment);

        boolean changed =
                payment.businessVersion() != previousVersion;

        if (!changed) {
            return PaymentWorkflowResult.from(
                    payment,
                    false
            );
        }

        List<PaymentDomainEvent> events =
                requireEvents(payment);

        Instant stagedAt = safeStagedAt(
                events,
                timeProvider.now()
        );

        Payment persisted = atomicPersistencePort.persist(
                payment,
                events,
                stagedAt
        );

        return PaymentWorkflowResult.from(
                persisted,
                true
        );
    }

    private static List<PaymentDomainEvent> requireEvents(
            Payment payment
    ) {
        List<PaymentDomainEvent> events =
                List.copyOf(payment.domainEvents());

        if (events.isEmpty()) {
            throw new IllegalStateException(
                    "A changed Payment must expose domain events"
            );
        }

        return events;
    }

    private static Instant safeStagedAt(
            List<PaymentDomainEvent> events,
            Instant currentTime
    ) {
        Objects.requireNonNull(currentTime, "Current time");

        Instant latestOccurrence = events.stream()
                .map(PaymentDomainEvent::occurredAt)
                .max(Instant::compareTo)
                .orElse(currentTime);

        return currentTime.isBefore(latestOccurrence)
                ? latestOccurrence
                : currentTime;
    }
}
