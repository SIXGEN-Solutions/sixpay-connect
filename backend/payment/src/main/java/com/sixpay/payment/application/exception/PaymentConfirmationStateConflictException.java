package com.sixpay.payment.application.exception;

/**
 * An OTP verification request is incompatible with the current Payment state.
 * This is a known functional conflict, not a system failure.
 */
public final class PaymentConfirmationStateConflictException extends RuntimeException {
    public PaymentConfirmationStateConflictException() {
        super("Payment is not awaiting OTP confirmation");
    }
}
