package com.sixpay.payment.application.view;

import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

/**
 * Optional bank-issued customer confirmation challenge.
 *
 * <p>This value remains absent until an authorized core-banking contract and
 * adapter provide the corresponding data.</p>
 */
public record PaymentConfirmationChallengeView(
        String bankOperationId,
        Money fees,
        Money netAmount,
        int validityInMinutes,
        String transactionNumber,
        String transactionQrCode
) {

    public PaymentConfirmationChallengeView {
        bankOperationId = requireText(
                bankOperationId,
                "Bank operation ID"
        );
        fees = Objects.requireNonNull(fees, "Fees");
        netAmount = Objects.requireNonNull(
                netAmount,
                "Net amount"
        );
        transactionNumber = requireText(
                transactionNumber,
                "Transaction number"
        );
        transactionQrCode = requireText(
                transactionQrCode,
                "Transaction QR code"
        );

        if (validityInMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Validity must be positive"
            );
        }

        if (!fees.currency().equals(
                netAmount.currency()
        )) {
            throw new IllegalArgumentException(
                    "Challenge amounts must use the same currency"
            );
        }
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
