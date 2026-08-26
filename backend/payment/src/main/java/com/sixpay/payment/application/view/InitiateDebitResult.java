package com.sixpay.payment.application.view;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable application result for the contracted InitiateDebit operation.
 *
 * <p>The confirmation challenge is optional until the core-banking contract is
 * approved. SIXPAY never fabricates bank identifiers, fees or QR data.</p>
 */
public record InitiateDebitResult(
        PaymentId paymentId,
        PublicPaymentReference paymentReference,
        String endToEndId,
        Money totalAmount,
        Instant initiatedAt,
        PaymentStatus status,
        PaymentConfirmationChallengeView confirmationChallenge
) {

    public InitiateDebitResult {
        paymentId = Objects.requireNonNull(
                paymentId,
                "Payment ID"
        );
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "Payment reference"
        );
        endToEndId = requireText(
                endToEndId,
                "End-to-end ID"
        );
        totalAmount = Objects.requireNonNull(
                totalAmount,
                "Total amount"
        );
        initiatedAt = Objects.requireNonNull(
                initiatedAt,
                "Initiated instant"
        );
        status = Objects.requireNonNull(
                status,
                "Payment status"
        );

        if (status != PaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalArgumentException(
                    "InitiateDebit result must be "
                            + "PENDING_CONFIRMATION"
            );
        }
    }

    public static InitiateDebitResult accepted(
            PaymentId paymentId,
            PublicPaymentReference paymentReference,
            String endToEndId,
            Money totalAmount,
            Instant initiatedAt
    ) {
        return new InitiateDebitResult(
                paymentId,
                paymentReference,
                endToEndId,
                totalAmount,
                initiatedAt,
                PaymentStatus.PENDING_CONFIRMATION,
                null
        );
    }

    public Optional<PaymentConfirmationChallengeView>
            optionalConfirmationChallenge() {
        return Optional.ofNullable(confirmationChallenge);
    }

    private static String requireText(
            String value,
            String label
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }

        return value.trim();
    }
}
