package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.evidence.PostingInstructionId;

import java.util.Objects;

public record PaymentPostingAuthorizationContext(
        PaymentStatus status,
        boolean authorizationAccepted,
        boolean bankingVerified,
        boolean fundsVerified,
        boolean fundsFresh,
        boolean treasuryResolved,
        PostingInstructionId currentInstructionId
) {
    public PaymentPostingAuthorizationContext {
        status = Objects.requireNonNull(status, "Payment status");
    }

    public boolean hasInstruction() {
        return currentInstructionId != null;
    }
}
