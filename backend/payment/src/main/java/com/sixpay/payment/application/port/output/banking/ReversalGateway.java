package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.evidence.ReversalAuthorizationEvidence;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;

import java.util.Objects;

public interface ReversalGateway {

    ReversalSnapshot reverse(ReversalRequest request);

    record ReversalRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            String bankPostingReference,
            ReversalAuthorizationEvidence authorization
    ) {
        public ReversalRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            context = Objects.requireNonNull(context, "Banking request context");
            idempotencyKey = Objects.requireNonNull(
                    idempotencyKey,
                    "Reversal idempotency key"
            );
            bankPostingReference = Objects.requireNonNull(
                    bankPostingReference,
                    "Bank posting reference"
            );
            if (bankPostingReference.isBlank()
                    || bankPostingReference.length() > 150) {
                throw new IllegalArgumentException(
                        "Bank posting reference must be non-blank "
                                + "and at most 150 characters"
                );
            }
            authorization = Objects.requireNonNull(
                    authorization,
                    "Reversal authorization evidence"
            );
        }
    }
}
