package com.sixpay.payment.application.view;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;

/**
 * Application-level Payment projection.
 *
 * <p>The view intentionally excludes debtor-account data and complete
 * evidence snapshots. Transport adapters may map this view to approved API
 * contracts in a later lot.</p>
 */
public record PaymentView(
        PaymentId paymentId,
        PublicPaymentReference publicPaymentReference,
        PaymentSource source,
        ExternalPaymentReference externalPaymentReference,
        ExternalSubscriptionReference externalSubscriptionReference,
        FinancialInstitutionCode financialInstitutionCode,
        Money requestedAmount,
        PaymentStatus status,
        long businessVersion,
        Instant receivedAt,
        Instant updatedAt,
        Instant finalizedAt
) {
    public PaymentView {
        paymentId = Objects.requireNonNull(paymentId, "Payment ID");
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        source = Objects.requireNonNull(source, "Payment source");
        externalPaymentReference = Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );
        externalSubscriptionReference = Objects.requireNonNull(
                externalSubscriptionReference,
                "External Subscription reference"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        requestedAmount = Objects.requireNonNull(
                requestedAmount,
                "Requested amount"
        );
        status = Objects.requireNonNull(status, "Payment status");

        if (businessVersion <= 0) {
            throw new IllegalArgumentException(
                    "Payment business version must be positive"
            );
        }

        receivedAt = Objects.requireNonNull(
                receivedAt,
                "Received instant"
        );
        updatedAt = Objects.requireNonNull(
                updatedAt,
                "Updated instant"
        );
    }

    public static PaymentView from(PaymentState state) {
        Objects.requireNonNull(state, "Payment state");

        return new PaymentView(
                state.paymentId(),
                state.publicPaymentReference(),
                state.source(),
                state.externalPaymentReference(),
                state.externalSubscriptionReference(),
                state.financialInstitutionCode(),
                state.requestedAmount(),
                state.status(),
                state.businessVersion(),
                state.receivedAt(),
                state.updatedAt(),
                state.finalizedAt().orElse(null)
        );
    }
}
