package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.application.port.output.PaymentAtomicPersistencePort;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
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

/**
 * Persists Payment state, audit records and outbox events atomically.
 */
@Component
public class PaymentAtomicPersistenceAdapter
        implements PaymentAtomicPersistencePort {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditAdapter auditAdapter;
    private final PaymentOutboxRepository outboxRepository;
    private final PaymentDomainEventMapper domainEventMapper;

    public PaymentAtomicPersistenceAdapter(
            PaymentRepository paymentRepository,
            PaymentAuditAdapter auditAdapter,
            PaymentOutboxRepository outboxRepository,
            PaymentDomainEventMapper domainEventMapper
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
    }

    /**
     * Writes the aggregate snapshot, append-only audit entries and pending
     * outbox records input one database transaction. Publishing is deliberately
     * deferred: relays can deliver the committed outbox records after the HTTP
     * request completes without risking an event for rolled-back state.
     */
    @Override
    @Transactional
    public Payment persist(
            Payment payment,
            List<PaymentDomainEvent> events,
            Instant stagedAt
    ) {
        Objects.requireNonNull(payment, "Payment");
        Objects.requireNonNull(events, "Payment events");
        Objects.requireNonNull(stagedAt, "Outbox staging instant");

        List<PaymentDomainEvent> immutableEvents =
                List.copyOf(events);

        if (immutableEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "Atomic Payment persistence requires events"
            );
        }

        Payment persisted =
                paymentRepository.save(payment);

        auditAdapter.appendAll(immutableEvents);

        List<PaymentOutboxEntity> outboxEntities =
                immutableEvents.stream()
                        .map(event ->
                                domainEventMapper.toOutboxEntity(
                                        event,
                                        stagedAt.isBefore(
                                                event.occurredAt()
                                        )
                                                ? event.occurredAt()
                                                : stagedAt
                                )
                        )
                        .toList();

        outboxRepository.saveAll(outboxEntities);
        outboxRepository.flush();

        return persisted;
    }
}
