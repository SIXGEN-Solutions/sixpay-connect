package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.evidence.TfjConfirmationId;

import java.util.Objects;

public record UniqueTfjMatchProof(
        TfjConfirmationId confirmationId,
        boolean durable,
        boolean unique,
        boolean conflictFree
) {
    public UniqueTfjMatchProof {
        confirmationId = Objects.requireNonNull(
                confirmationId,
                "TFJ confirmation ID"
        );
    }

    public boolean isConclusive() {
        return durable && unique && conflictFree;
    }
}
