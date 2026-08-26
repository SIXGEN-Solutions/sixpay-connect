package com.sixpay.payment.application.reconciliation;

import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record PostingReconciliationResult(
        ReconciliationDisposition disposition,
        Optional<PostingOutcomeSnapshot> snapshot,
        Instant evaluatedAt
) {
    public PostingReconciliationResult {
        disposition = Objects.requireNonNull(
                disposition,
                "Reconciliation disposition"
        );
        snapshot = Objects.requireNonNullElseGet(
                snapshot,
                Optional::empty
        );
        evaluatedAt = Objects.requireNonNull(
                evaluatedAt,
                "Evaluation instant"
        );
    }
}
