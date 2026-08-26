package com.sixpay.accounting.domain.policy;

import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;

import java.util.Objects;

public final class VerifiedTresorPayStatusEligibilityPolicy
        implements AccountingEligibilityPolicy {

    @Override
    public AccountingEligibilityDecision evaluate(
            AccountingPaymentCandidate candidate,
            AccountingSelectionWindow window
    ) {
        Objects.requireNonNull(
                candidate,
                "candidate"
        );

        Objects.requireNonNull(
                window,
                "window"
        );

        if (!window.contains(
                candidate.paymentOccurredAt()
        )) {
            return AccountingEligibilityDecision.rejected(
                    "PAYMENT_OUTSIDE_ACCOUNTING_WINDOW"
            );
        }

        if (candidate.tresorPayStatusEvidence()
                .checkedAt()
                .isAfter(window.toExclusive())) {
            return AccountingEligibilityDecision.rejected(
                    "TRESORPAY_STATUS_CHECKED_AFTER_CUTOFF"
            );
        }

        return AccountingEligibilityDecision.accepted();
    }
}