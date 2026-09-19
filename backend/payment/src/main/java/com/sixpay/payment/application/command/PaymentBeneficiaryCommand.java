package com.sixpay.payment.application.command;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One Treasury beneficiary allocation supplied with a Treasury-payment
 * initiation.
 *
 * <p>This command type is not a universal Payment beneficiary abstraction.
 * It is translated to {@code TreasuryAllocationIntent} before aggregate
 * creation.</p>
 */
public record PaymentBeneficiaryCommand(
        String rib,
        BigDecimal amount
) {

    public PaymentBeneficiaryCommand {
        rib = requireText(rib, 64, "Beneficiary RIB");
        amount = Objects.requireNonNull(
                amount,
                "Beneficiary amount"
        );

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Beneficiary amount must be positive"
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Beneficiary amount must have at most 2 decimals"
            );
        }
    }

    private static String requireText(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    label + " must not be null"
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " exceeds "
                            + maximumLength
                            + " characters"
            );
        }

        return normalized;
    }
}
