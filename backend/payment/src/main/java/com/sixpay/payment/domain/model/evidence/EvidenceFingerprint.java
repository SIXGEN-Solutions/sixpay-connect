package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

public record EvidenceFingerprint(String value) implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile("^v1:sha256:[0-9a-f]{64}$");

    public EvidenceFingerprint {
        value = EvidenceValueObjectRules.requirePattern(
                value, FORMAT, 74, 74, "Evidence fingerprint"
        );
    }

    public static EvidenceFingerprint of(String value) {
        return new EvidenceFingerprint(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
