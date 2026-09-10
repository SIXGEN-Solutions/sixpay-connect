package com.sixpay.payment.application.service;

import com.sixpay.payment.application.model.FundsControlRequest;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.FundsControlCheckType;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the internal Funds Control request exclusively from durable Payment
 * state and the canonical payment-mvp/v1 check baseline.
 */
public final class PaymentFundsControlRequestFactory {

    public FundsControlRequest create(
            PaymentState state,
            Instant requestedAt
    ) {
        Objects.requireNonNull(state, "Payment state");
        Objects.requireNonNull(requestedAt, "Funds-control request instant");

        if (state.status() != PaymentStatus.FUNDS_CONTROL_PENDING) {
            throw new IllegalArgumentException(
                    "Funds Control request requires FUNDS_CONTROL_PENDING"
            );
        }

        return new FundsControlRequest(
                state.paymentId(),
                state.publicPaymentReference(),
                state.financialInstitutionCode(),
                state.requestedAmount(),
                state.debtorAccountReference().bindingFingerprint(),
                Set.of(FundsControlCheckType.values()),
                requestedAt
        );
    }
}
