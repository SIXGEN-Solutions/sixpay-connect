package com.sixpay.payment.application.port.output.idempotency;

import com.sixpay.payment.application.port.output.banking.PaymentConfirmationBankResult;

import java.util.Objects;

/**
 * Result of an idempotent Payment-confirmation mutation.
 *
 * <p>The replay metadata is technical transport information. It does not alter
 * the Payment-confirmation business result.</p>
 */
public record PaymentConfirmationIdempotencyResult(
        PaymentConfirmationBankResult result,
        boolean replayed
) {

    public PaymentConfirmationIdempotencyResult {
        result = Objects.requireNonNull(
                result,
                "Payment confirmation bank result"
        );
    }

    public static PaymentConfirmationIdempotencyResult executed(
            PaymentConfirmationBankResult result
    ) {
        return new PaymentConfirmationIdempotencyResult(result, false);
    }

    public static PaymentConfirmationIdempotencyResult replayed(
            PaymentConfirmationBankResult result
    ) {
        return new PaymentConfirmationIdempotencyResult(result, true);
    }
}
