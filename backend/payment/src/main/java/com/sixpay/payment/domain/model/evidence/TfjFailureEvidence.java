package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public record TfjFailureEvidence(
        FailureCode code,
        TfjRecoveryAction recoveryAction
) implements ValueObject {

    public TfjFailureEvidence {
        code = EvidenceValueObjectRules.requireNonNull(
                code,
                "TFJ failure code"
        );
        recoveryAction = EvidenceValueObjectRules.requireNonNull(
                recoveryAction,
                "TFJ recovery action"
        );
    }
}
