package com.sixpay.payment.domain.model.evidence;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

/**
 * SHA-256 fingerprint used to bind immutable Payment evidence.
 *
 * @param value canonical fingerprint
 */
public record EvidenceFingerprint(
        String value
) implements ValueObject {

    private static final Pattern FORMAT =
            Pattern.compile(
                    "^v1:sha256:[0-9a-f]{64}$"
            );

    public EvidenceFingerprint {
        if (value == null
                || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Evidence fingerprint must use format "
                            + "v1:sha256:<64 lowercase hexadecimal characters>"
            );
        }
    }

    public static EvidenceFingerprint of(String value) {
        return new EvidenceFingerprint(value);
    }

    @Override
    public String toString() {
        return value;
    }
}