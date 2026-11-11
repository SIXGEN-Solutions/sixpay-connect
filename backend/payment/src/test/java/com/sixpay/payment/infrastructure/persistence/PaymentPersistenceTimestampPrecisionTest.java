package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.payment.domain.model.PaymentState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression: PostgreSQL TIMESTAMPTZ (microseconds) and Payment JSON must
 * have exactly matching indexed timestamps. No read-time tolerance exists.
 */
class PaymentPersistenceTimestampPrecisionTest {

    @Test
    void receiptAndUpdateUseTheSameMicrosecondValuesInEntityAndDocument() {
        Instant precise = Instant.parse("2026-11-03T14:06:55.851911800Z");
        PaymentState state = PaymentAggregateTestFixtures.newPayment()
                .toState().toBuilder()
                .receivedAt(precise)
                .updatedAt(precise)
                .build();

        PaymentStateDocument document = PaymentStateDocument.from(state);
        PaymentJpaEntity entity = PaymentJpaEntity.create(state, "{}");

        assertEquals(Instant.parse("2026-11-03T14:06:55.851911Z"),
                document.receivedAt());
        assertEquals(Instant.parse("2026-11-03T14:06:55.851912Z"),
                document.updatedAt());
        assertEquals(document.receivedAt(), entity.receivedAt());
        assertEquals(document.updatedAt(), entity.updatedAt());
        assertEquals(document.receivedAt(), document.toState().receivedAt());
        assertEquals(document.updatedAt(), document.toState().updatedAt());
    }

    @Test
    void updatingTheEntityPreservesExactTimestampProjection() {
        Instant received = Instant.parse("2026-11-03T14:06:55.851911800Z");
        Instant updated = Instant.parse("2026-11-03T14:06:56.123456789Z");

        PaymentState original = PaymentAggregateTestFixtures.newPayment()
                .toState().toBuilder()
                .receivedAt(received)
                .updatedAt(received)
                .build();
        PaymentJpaEntity entity = PaymentJpaEntity.create(original, "original");

        PaymentState changed = original.toBuilder()
                .businessVersion(original.businessVersion() + 1)
                .updatedAt(updated)
                .build();
        PaymentStateDocument document = PaymentStateDocument.from(changed);
        entity.synchronize(changed, "updated");

        assertEquals(document.updatedAt(), entity.updatedAt());
        assertEquals(Instant.parse("2026-11-03T14:06:56.123457Z"),
                entity.updatedAt());
        assertEquals(document.receivedAt(), entity.receivedAt());
    }

    @Test
    void normalizationIsIdempotentAndHandlesAbsentFinalization() {
        Instant alreadyMicro = Instant.parse("2026-11-03T14:06:56.123456Z");
        assertEquals(alreadyMicro, PaymentStateDocument.sqlReceivedAt(alreadyMicro));
        assertEquals(alreadyMicro,
                PaymentStateDocument.sqlUpdatedOrFinalizedAt(alreadyMicro));
        assertNull(PaymentStateDocument.sqlUpdatedOrFinalizedAt(null));
    }
}
