package com.sixpay.accounting.domain.policy;

import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;

public interface AccountingEligibilityPolicy {
    AccountingEligibilityDecision evaluate(
            AccountingPaymentCandidate candidate,
            AccountingSelectionWindow window
    );
}
