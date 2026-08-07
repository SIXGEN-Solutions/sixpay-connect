package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.FundsReleaseSnapshot;
import com.sixpay.payment.domain.model.evidence.FundsReservationReference;

import java.util.Objects;

public interface FundsReleaseGateway {

    FundsReleaseSnapshot release(FundsReleaseRequest request);

    record FundsReleaseRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            FundsReservationReference reservationReference,
            String reasonCode
    ) {
        public FundsReleaseRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            context = Objects.requireNonNull(context, "Banking request context");
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Release idempotency key"
            );
            reservationReference = Objects.requireNonNull(
                    reservationReference,
                    "Reservation reference"
            );
            if (reasonCode == null
                    || reasonCode.isBlank()
                    || reasonCode.length() > 64) {
                throw new IllegalArgumentException(
                        "Release reason code must be non-blank and at most 64 characters"
                );
            }
            reasonCode = reasonCode.strip().toUpperCase();
        }
    }
}
