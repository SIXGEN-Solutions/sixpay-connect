package com.sixpay.customer.verification.domain.service;

import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;
import com.sixpay.customer.verification.domain.policy.VerificationOutcomePolicy;

import java.util.Collection;

public final class CustomerVerificationDecisionService {
    public VerificationOutcome decide(Collection<VerificationCheck> checks) {
        return VerificationOutcomePolicy.determine(checks);
    }
}
