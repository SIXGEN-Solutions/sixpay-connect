package com.sixpay.payment.application.view;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Stable application result for the contracted InitiateDebit operation.
 */
public record InitiateDebitResult(
        PaymentId paymentId,
        PublicPaymentReference paymentReference,
        String endToEndId,
        String bankOperationId,
        Money totalAmount,
        Money fees,
        Money netAmount,
        Instant initiatedAt,
        int validityInMinutes,
        String transactionNumber,
        String transactionQrCode,
        PaymentStatus status
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
        bankOperationId = requireText(
                bankOperationId,
                "Bank operation ID"
        );
        totalAmount = Objects.requireNonNull(
                totalAmount,
                "Total amount"
        );
        fees = Objects.requireNonNull(
                fees,
                "Fees"
        );
        netAmount = Objects.requireNonNull(
                netAmount,
                "Net amount"
        );
        initiatedAt = Objects.requireNonNull(
                initiatedAt,
                "Initiated instant"
        );
        transactionNumber = requireText(
                transactionNumber,
                "Transaction number"
        );
        transactionQrCode = requireText(
                transactionQrCode,
                "Transaction QR code"
        );
        status = Objects.requireNonNull(
                status,
                "Payment status"
        );

        if (validityInMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Validity must be positive"
            );
        }

        if (status != PaymentStatus.PENDING_CONFIRMATION) {
            throw new IllegalArgumentException(
                    "InitiateDebit result must be "
                            + "PENDING_CONFIRMATION"
            );
        }

        if (!totalAmount.currency().equals(
                fees.currency()
        )
                || !totalAmount.currency().equals(
                        netAmount.currency()
                )) {
            throw new IllegalArgumentException(
                    "All response amounts must use the same currency"
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
