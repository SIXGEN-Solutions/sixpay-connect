package com.sixpay.payment.domain.event;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract for the 33 explicit events owned by Payment.
 */
public interface PaymentDomainEvent extends DomainEvent {

    PaymentEventMetadata metadata();

    @Override
    default UUID eventId() {
        return metadata().eventId();
    }

    default PaymentId paymentId() {
        return metadata().paymentId();
    }

    default PublicPaymentReference paymentReference() {
        return metadata().paymentReference();
    }

    default CorrelationId correlationId() {
        return metadata().correlationId();
    }

    default PaymentStatus paymentStatus() {
        return metadata().paymentStatus();
    }

    default long aggregateVersion() {
        return metadata().aggregateVersion();
    }

    default int eventSequence() {
        return metadata().eventSequence();
    }

    default UUID causationId() {
        return metadata().causationId();
    }

    @Override
    default Instant occurredAt() {
        return metadata().occurredAt();
    }
}
