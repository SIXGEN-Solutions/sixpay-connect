package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.FundsReservationSnapshot;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public interface FundsReservationGateway {

    FundsReservationSnapshot reserve(
            FundsReservationRequest request
    );

    record FundsReservationRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            String debtorAccountReference,
            Money amount
    ) {
        public FundsReservationRequest {
            paymentId = Objects.requireNonNull(
                    paymentId,
                    "Payment ID"
            );
            context = Objects.requireNonNull(
                    context,
                    "Banking request context"
            );
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Banking idempotency key"
            );
            if (debtorAccountReference == null
                    || debtorAccountReference.isBlank()
                    || debtorAccountReference.length() > 100) {
                throw new IllegalArgumentException(
                        "Debtor account reference must be non-blank "
                                + "and at most 100 characters"
                );
            }
            amount = Objects.requireNonNull(
                    amount,
                    "Reservation amount"
            );
            if (!amount.isPositive()) {
                throw new IllegalArgumentException(
                        "Reservation amount must be positive"
                );
            }
        }
    }
}
