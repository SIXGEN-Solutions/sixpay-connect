package com.sixpay.payment.application.model;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Provider-neutral Payment-owned request for execution-time Funds Control.
 *
 * <p>This is not an Amplitude wire payload and must not be treated as one.
 * Provider mapping remains outside this internal baseline until the applicable
 * physical Core Banking contract permits generation.</p>
 */
public record FundsControlRequest(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        FinancialInstitutionCode financialInstitutionCode,
        Money requestedAmount,
        String debtorAccountBindingFingerprint,
        Set<FundsControlCheckType> requiredChecks,
        Instant requestedAt
) {
    public FundsControlRequest {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        requestedAmount = Objects.requireNonNull(
                requestedAmount,
                "Requested amount"
        );
        if (!requestedAmount.isPositive()) {
            throw new IllegalArgumentException(
                    "Funds-control requested amount must be positive"
            );
        }

        debtorAccountBindingFingerprint = Objects.requireNonNull(
                debtorAccountBindingFingerprint,
                "Debtor account binding fingerprint"
        ).strip();
        if (!debtorAccountBindingFingerprint.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Debtor account binding fingerprint has an invalid format"
            );
        }

        Objects.requireNonNull(requiredChecks, "Required funds-control checks");
        requiredChecks = Set.copyOf(requiredChecks);
        if (!requiredChecks.equals(Set.of(FundsControlCheckType.values()))) {
            throw new IllegalArgumentException(
                    "Funds Control requires the complete payment-mvp/v1 check set"
            );
        }

        requestedAt = Objects.requireNonNull(
                requestedAt,
                "Funds-control request instant"
        );
    }
}
