package com.sixpay.payment.infrastructure.audit;

import com.sixpay.payment.domain.event.PaymentDomainEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Appends immutable Payment domain-event metadata to the audit timeline.
 *
 * <p>Insertion is idempotent by event ID. The mandatory transaction ensures
 * audit entries cannot commit independently from the Payment state and outbox
 * records produced by the same mutation.</p>
 */
@Repository
public class PaymentAuditAdapter {

    private final PaymentAuditRepository repository;

    public PaymentAuditAdapter(PaymentAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(PaymentDomainEvent event) {
        Objects.requireNonNull(event, "Payment domain event");
        if (repository.existsByEventId(event.eventId())) {
            return;
        }
        try {
            repository.save(PaymentAuditEntity.from(event));
        } catch (DataIntegrityViolationException exception) {
            if (!repository.existsByEventId(event.eventId())) {
                throw exception;
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAll(List<? extends PaymentDomainEvent> events) {
        Objects.requireNonNull(events, "Payment domain events");
        for (PaymentDomainEvent event : List.copyOf(events)) {
            append(event);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentAuditEntry> findByPaymentId(UUID paymentId) {
        return repository
                .findByPaymentIdOrderByBusinessVersionAscEventSequenceAsc(
                        Objects.requireNonNull(paymentId)
                )
                .stream()
                .map(PaymentAuditEntry::from)
                .toList();
    }
}
