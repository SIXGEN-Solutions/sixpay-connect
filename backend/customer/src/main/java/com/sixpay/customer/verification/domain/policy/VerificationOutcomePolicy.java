package com.sixpay.customer.verification.domain.policy;

import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckResult;
import com.sixpay.customer.verification.domain.model.VerificationOutcome;

import java.util.Collection;
import java.util.List;

public final class VerificationOutcomePolicy {
    private VerificationOutcomePolicy() {}
    public static VerificationOutcome determine(Collection<VerificationCheck> checks) {
        List<VerificationCheck> canonical = RequiredVerificationChecksPolicy.requireComplete(checks);
        if (canonical.stream().anyMatch(c -> c.result() == VerificationCheckResult.FAIL)) return VerificationOutcome.REJECTED;
        if (canonical.stream().anyMatch(c -> c.result() == VerificationCheckResult.UNKNOWN)) return VerificationOutcome.INDETERMINATE;
        return VerificationOutcome.VERIFIED;
    }
}
