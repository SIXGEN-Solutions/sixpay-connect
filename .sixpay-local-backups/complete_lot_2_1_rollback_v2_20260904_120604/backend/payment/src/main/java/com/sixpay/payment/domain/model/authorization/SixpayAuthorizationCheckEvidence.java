package com.sixpay.payment.domain.model.authorization;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.Objects;

public record SixpayAuthorizationCheckEvidence(
        SixpayAuthorizationCheck check,
        SixpayAuthorizationCheckResult result
) implements ValueObject {

    public SixpayAuthorizationCheckEvidence {
        check = Objects.requireNonNull(check, "Authorization check");
        result = Objects.requireNonNull(result, "Authorization check result");
    }
}
