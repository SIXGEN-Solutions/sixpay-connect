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
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(window, "window");

        // Carry-over: older successful, unbatched T0 events remain eligible.
        if (!candidate.paymentOccurredAt().isBefore(window.toExclusive())) {
            return AccountingEligibilityDecision.rejected(
                    "PAYMENT_OUTSIDE_ACCOUNTING_WINDOW"
            );
        }

        if (!candidate.tresorPayStatusEvidence().confirmsPaidPayment()) {
            return AccountingEligibilityDecision.rejected(
                    "TRESORPAY_PAYMENT_NOT_CONFIRMED_PAID"
            );
        }

        // Partner confirmation is obtained at T1; it may occur after
        // the original payment's cutoff, including for carry-over candidates.

        return AccountingEligibilityDecision.accepted();
    }
}
