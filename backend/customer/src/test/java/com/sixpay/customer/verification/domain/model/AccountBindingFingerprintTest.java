package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountBindingFingerprintTest {

    private static final String VALID =
            "v1:" + "a".repeat(64);

    @Test
    void acceptsTheCanonicalPaymentCompatibleFormat() {
        AccountBindingFingerprint fingerprint =
                AccountBindingFingerprint.of(VALID);

        assertEquals(VALID, fingerprint.value());
    }

    @Test
    void hidesTheFingerprintFromToString() {
        AccountBindingFingerprint fingerprint =
                AccountBindingFingerprint.of(VALID);

        assertEquals(
                "[PROTECTED_ACCOUNT_BINDING]",
                fingerprint.toString()
        );
    }

    @Test
    void rejectsWrongVersionLengthCaseAndCharacters() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> AccountBindingFingerprint.of(
                        "v2:" + "a".repeat(64)
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(63)
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> AccountBindingFingerprint.of(
                        "v1:" + "A".repeat(64)
                )
        );
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> AccountBindingFingerprint.of(
                        "v1:" + "g".repeat(64)
                )
        );
    }
}
