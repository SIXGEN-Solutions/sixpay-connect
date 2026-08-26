package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;

import java.util.List;

public interface PaymentAccountingCandidateSource {

    List<AccountingPaymentCandidate>
    findUnbatchedStatusVerifiedCandidates(
            AccountingSelectionWindow window
    );
}
