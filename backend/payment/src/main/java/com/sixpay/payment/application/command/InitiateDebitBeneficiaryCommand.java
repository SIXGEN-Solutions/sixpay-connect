package com.sixpay.payment.application.command;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One beneficiary allocation supplied with an InitiateDebit command.
 */
public record InitiateDebitBeneficiaryCommand(
        String rib,
        BigDecimal amount
) {

    public InitiateDebitBeneficiaryCommand {
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
