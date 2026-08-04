package com.sixpay.payment.application.exception;

import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;
import com.sixpay.payment.domain.model.PaymentId;

import java.util.Objects;
import java.util.UUID;

/**
 * Signals that the durable Payment banking-verification workflow must be
 * retried without changing the Payment aggregate.
 */
public final class PaymentCustomerVerificationRetryableException
        extends RuntimeException {

    private final PaymentId paymentId;
    private final UUID customerVerificationId;
    private final CustomerVerificationTechnicalException.ErrorType errorType;

    public PaymentCustomerVerificationRetryableException(
            PaymentId paymentId,
            UUID customerVerificationId,
            CustomerVerificationTechnicalException.ErrorType errorType,
            Throwable cause
    ) {
        super(
                "Customer verification is temporarily unavailable for payment "
                        + Objects.requireNonNull(
                                paymentId,
                                "paymentId is required"
                        ).value(),
                cause
        );
        this.paymentId = paymentId;
        this.customerVerificationId = Objects.requireNonNull(
                customerVerificationId,
                "customerVerificationId is required"
        );
        this.errorType = Objects.requireNonNull(
                errorType,
                "errorType is required"
        );
    }

    public PaymentId paymentId() {
        return paymentId;
    }

    public UUID customerVerificationId() {
        return customerVerificationId;
    }

    public CustomerVerificationTechnicalException.ErrorType errorType() {
        return errorType;
    }

    public boolean retryable() {
        return true;
    }
}
