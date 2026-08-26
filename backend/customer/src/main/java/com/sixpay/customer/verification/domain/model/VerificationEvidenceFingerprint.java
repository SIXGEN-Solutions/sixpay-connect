package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.util.regex.Pattern;

public record VerificationEvidenceFingerprint(String value) implements ValueObject {
    private static final Pattern FORMAT = Pattern.compile("^v1:sha256:[0-9a-f]{64}$");
    public VerificationEvidenceFingerprint {
        if (value == null || !FORMAT.matcher(value).matches())
            throw new CustomerVerificationDomainException("Verification evidence fingerprint must use format v1:sha256:<64 lowercase hexadecimal characters>");
    }
    public static VerificationEvidenceFingerprint of(String value) { return new VerificationEvidenceFingerprint(value); }
    @Override public String toString() { return value; }
}
