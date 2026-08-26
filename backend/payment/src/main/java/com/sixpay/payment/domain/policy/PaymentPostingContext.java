package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.PostingIdempotencyKey;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public record PaymentPostingContext(
        PaymentStatus status,
        PostingInstructionId instructionId,
        PostingIdempotencyKey idempotencyKey,
        Money amount
) {
    public PaymentPostingContext {
        status = Objects.requireNonNull(status, "Payment status");
        instructionId = Objects.requireNonNull(
                instructionId,
                "Posting instruction ID"
        );
        idempotencyKey = Objects.requireNonNull(
                idempotencyKey,
                "Posting idempotency key"
        );
        amount = Objects.requireNonNull(amount, "Payment amount");
    }
}
