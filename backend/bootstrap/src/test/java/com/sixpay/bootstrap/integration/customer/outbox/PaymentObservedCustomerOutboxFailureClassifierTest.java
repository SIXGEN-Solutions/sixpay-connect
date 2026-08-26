package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxSerializationException;
import com.sixpay.payment.infrastructure.outbox.serialization.UnknownPaymentOutboxEventTypeException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentObservedCustomerOutboxFailureClassifierTest {

    private final PaymentObservedCustomerOutboxFailureClassifier classifier =
            new PaymentObservedCustomerOutboxFailureClassifier();

    @Test
    void classifiesWrappedSerializationFailureAsNonRetryable() {
        var classification = classifier.classify(
                new IllegalStateException(
                        "wrapper",
                        new PaymentOutboxSerializationException(
                                "invalid payload"
                        )
                )
        );

        assertFalse(classification.retryable());
        assertEquals("invalid_event_payload", classification.errorType());
    }

    @Test
    void classifiesUnknownTypeAsNonRetryable() {
        var classification = classifier.classify(
                new UnknownPaymentOutboxEventTypeException(
                        "payment.unknown"
                )
        );

        assertFalse(classification.retryable());
        assertEquals("unknown_event_type", classification.errorType());
    }

    @Test
    void classifiesTemporaryPersistenceFailureAsRetryable() {
        var classification = classifier.classify(
                new DataAccessResourceFailureException("temporary")
        );

        assertTrue(classification.retryable());
        assertEquals(
                "temporary_persistence_failure",
                classification.errorType()
        );
    }
}
