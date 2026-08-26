package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

public record AuthorizationEvidenceReference(String value)
        implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^v1:hmac-sha256:[0-9a-f]{64}$");

    public AuthorizationEvidenceReference {
        value = EvidenceValueObjectRules.requirePattern(
                value,
                FORMAT,
                79,
                79,
                "Authorization evidence reference"
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
