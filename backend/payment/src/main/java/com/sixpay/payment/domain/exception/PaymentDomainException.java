package com.sixpay.payment.domain.exception;

import com.sixpay.payment.domain.model.PaymentStatus;

/**
 * Stable domain exception raised before any Payment mutation.
 */
public final class PaymentDomainException extends RuntimeException {

    private final String code;

    public PaymentDomainException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment domain error code must not be blank"
            );
        }
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static PaymentDomainException invalidTransition(
            PaymentStatus status,
            String operation
    ) {
        return new PaymentDomainException(
                "PAYMENT_INVALID_TRANSITION",
                operation + " is not allowed from " + status
        );
    }

    public static PaymentDomainException conflict(String message) {
        return new PaymentDomainException(
                "PAYMENT_EVIDENCE_CONFLICT",
                message
        );
    }

    public static PaymentDomainException rejected(
            String reasonCode
    ) {
        return new PaymentDomainException(
                "PAYMENT_POLICY_REJECTED",
                reasonCode
        );
    }
}
