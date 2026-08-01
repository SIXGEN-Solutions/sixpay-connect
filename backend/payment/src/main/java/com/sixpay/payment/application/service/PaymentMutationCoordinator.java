package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.repository.PaymentRepository;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.outbox.PaymentDomainEventMapper;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared transaction boundary for focused Payment workflow services.
 *
 * <p>The coordinator centralizes persistence mechanics only. All lifecycle
 * decisions remain inside the Payment Aggregate Root.</p>
 */
@Component
public class PaymentMutationCoordinator {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditAdapter auditAdapter;
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentDomainEventMapper domainEventMapper;
    private final TimeProvider timeProvider;

    public PaymentMutationCoordinator(
            PaymentRepository paymentRepository,
            PaymentAuditAdapter auditAdapter,
            PaymentOutboxRepository outboxRepository,
            PaymentDomainEventMapper domainEventMapper,
            TimeProvider timeProvider
    ) {
        this.paymentRepository = Objects.requireNonNull(
                paymentRepository,
                "Payment repository"
        );
        this.auditAdapter = Objects.requireNonNull(
                auditAdapter,
                "Payment audit adapter"
        );
        this.outboxRepository = Objects.requireNonNull(
                outboxRepository,
                "Payment outbox repository"
        );
        this.domainEventMapper = Objects.requireNonNull(
                domainEventMapper,
                "Payment domain-event mapper"
        );
        this.timeProvider = Objects.requireNonNull(
                timeProvider,
                "Time provider"
        );
    }

    @Transactional
    public PaymentWorkflowResult persistNew(Payment payment) {
        Objects.requireNonNull(payment, "Payment");

        List<PaymentDomainEvent> events =
                requireEvents(payment);

        paymentRepository.save(payment);
        persistSideEffects(events);

        return PaymentWorkflowResult.from(
                payment,
                true
        );
    }

    @Transactional
    public PaymentWorkflowResult mutate(
            PaymentId paymentId,
            Consumer<Payment> mutation
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(mutation, "Payment mutation");

        Payment payment = paymentRepository
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

        paymentRepository.save(payment);
        persistSideEffects(events);

        return PaymentWorkflowResult.from(
                payment,
                true
        );
    }

    private void persistSideEffects(
            List<PaymentDomainEvent> events
    ) {
        auditAdapter.appendAll(events);

        Instant createdAt = timeProvider.now();

        List<PaymentOutboxEntity> outboxEntities =
                events.stream()
                        .map(event ->
                                domainEventMapper.toOutboxEntity(
                                        event,
                                        createdAt.isBefore(
                                                event.occurredAt()
                                        )
                                                ? event.occurredAt()
                                                : createdAt
                                )
                        )
                        .toList();

        outboxRepository.saveAll(outboxEntities);
        outboxRepository.flush();
    }

    private static List<PaymentDomainEvent> requireEvents(
            Payment payment
    ) {
        List<PaymentDomainEvent> events =
                payment.domainEvents();

        if (events.isEmpty()) {
            throw new IllegalStateException(
                    "A changed Payment must expose domain events"
            );
        }

        return events;
    }
}
