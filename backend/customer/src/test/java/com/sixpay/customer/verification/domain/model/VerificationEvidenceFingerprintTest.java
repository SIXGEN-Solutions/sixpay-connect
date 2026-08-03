package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VerificationEvidenceFingerprintTest {
    @Test void acceptsCanonicalFormat() {
        String v = "v1:sha256:" + "a".repeat(64);
        assertEquals(v, VerificationEvidenceFingerprint.of(v).value());
    }
    @Test void rejectsInvalidValues() {
        for (String v : new String[]{"v2:sha256:"+"a".repeat(64), "v1:sha512:"+"a".repeat(64), "v1:sha256:"+"A".repeat(64), "v1:sha256:"+"a".repeat(63)})
            assertThrows(CustomerVerificationDomainException.class, () -> VerificationEvidenceFingerprint.of(v));
    }
}
