package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.IdempotencyKey;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.util.Arrays;
import java.util.Objects;

/**
 * Public verify-confirmation application command.
 *
 * <p>The OTP is transient in-memory input only. The record performs a
 * defensive copy on construction and access, and its textual representation
 * always redacts the OTP.</p>
 */
public record VerifyPaymentConfirmationCommand(
        PublicPaymentReference paymentReference,
        CorrelationId correlationId,
        IdempotencyKey idempotencyKey,
        char[] otp
) {

    public VerifyPaymentConfirmationCommand {
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Idempotency key"
        );

        Objects.requireNonNull(otp, "OTP");
        if (otp.length == 0) {
            throw new IllegalArgumentException(
                    "OTP must not be empty"
            );
        }

        otp = Arrays.copyOf(otp, otp.length);
    }

    /**
     * Returns a defensive copy so the record never exposes its internal
     * transient OTP array.
     */
    @Override
    public char[] otp() {
        return Arrays.copyOf(otp, otp.length);
    }

    @Override
    public String toString() {
        return "VerifyPaymentConfirmationCommand[paymentReference="
                + paymentReference
                + ", correlationId="
                + correlationId
                + ", idempotencyKey=<opaque>, otp=<redacted>]";
    }
}
