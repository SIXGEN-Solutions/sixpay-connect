package com.sixpay.payment.infrastructure.audit;

import com.sixpay.payment.domain.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentAuditEntry(
        UUID eventId,
        UUID paymentId,
        String publicPaymentReference,
        String eventType,
        PaymentStatus paymentStatus,
        long businessVersion,
        int eventSequence,
        String correlationId,
        UUID causationId,
        Instant occurredAt
) {
    static PaymentAuditEntry from(PaymentAuditEntity entity) {
        return new PaymentAuditEntry(
                entity.eventId(),
                entity.paymentId(),
                entity.publicPaymentReference(),
                entity.eventType(),
                entity.paymentStatus(),
                entity.businessVersion(),
                entity.eventSequence(),
                entity.correlationId(),
                entity.causationId(),
                entity.occurredAt()
        );
    }
}
