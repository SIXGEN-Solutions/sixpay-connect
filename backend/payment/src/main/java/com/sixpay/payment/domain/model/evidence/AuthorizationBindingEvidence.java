package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

public record AuthorizationBindingEvidence(
        AuthorizationBindingType type,
        AuthorizationBindingResult result
) implements ValueObject {

    public AuthorizationBindingEvidence {
        type = EvidenceValueObjectRules.requireNonNull(
                type,
                "Authorization binding type"
        );
        result = EvidenceValueObjectRules.requireNonNull(
                result,
                "Authorization binding result"
        );
    }
}
