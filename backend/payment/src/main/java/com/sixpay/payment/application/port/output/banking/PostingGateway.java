package com.sixpay.payment.application.port.output.banking;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public interface PostingGateway {

    PostingOutcomeSnapshot post(PostingRequest request);

    record PostingRequest(
            PaymentId paymentId,
            BankingRequestContext context,
            BankingIdempotencyKey idempotencyKey,
            String debtorAccountReference,
            TreasuryAccountReference treasuryAccountReference,
            Money amount
    ) {
        public PostingRequest {
            paymentId = Objects.requireNonNull(paymentId, "Payment ID");
            context = Objects.requireNonNull(context, "Banking request context");
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
            treasuryAccountReference = Objects.requireNonNull(
                    treasuryAccountReference,
                    "Treasury account reference"
            );
            amount = Objects.requireNonNull(amount, "Payment amount");
            if (!amount.isPositive()) {
                throw new IllegalArgumentException(
                        "Payment amount must be positive"
                );
            }
        }
    }
}
