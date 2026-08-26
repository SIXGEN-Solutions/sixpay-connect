package com.sixpay.payment.domain.model;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

/**
 * Original request identity protected by Payment.
 *
 * @param idempotencyKey submission identity
 * @param requestFingerprint canonical request digest
 * @param correlationId end-to-end trace identity
 */
public record PaymentRequestIdentity(
        IdempotencyKey idempotencyKey,
        RequestFingerprint requestFingerprint,
        CorrelationId correlationId
) implements ValueObject {

    public PaymentRequestIdentity {
        idempotencyKey =
                PaymentValueObjectRules.requireNonNull(
                        idempotencyKey,
                        "Idempotency key"
                );
        requestFingerprint =
                PaymentValueObjectRules.requireNonNull(
                        requestFingerprint,
                        "Request fingerprint"
                );
        correlationId =
                PaymentValueObjectRules.requireNonNull(
                        correlationId,
                        "Correlation ID"
                );

        PaymentValueObjectRules.parseCanonicalUuid(
                correlationId.value(),
                "Correlation ID"
        );
    }

    @Override
    public String toString() {
        return "PaymentRequestIdentity[correlationId="
                + correlationId + "]";
    }
}
