package com.sixpay.payment.infrastructure.idempotency;

/**
 * Raised when the same operation and idempotency key are reused with a
 * different canonical Payment request.
 */
public final class PaymentIdempotencyConflictException
        extends RuntimeException {

    public PaymentIdempotencyConflictException(
            String operation,
            String idempotencyKey
    ) {
        super(
                "Idempotency key conflict for operation "
                        + operation
                        + " and key "
                        + idempotencyKey
        );
    }
}
