package com.sixpay.customer.verification.application.port.output;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.VerificationCheck;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.domain.model.VerificationEvidenceFingerprint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankingVerificationResponseTest {

    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T14:00:01Z");

    @Test
    void exposesCompleteImmutableCustomerNativeEvidence() {
        ArrayList<VerificationCheck> mutable =
                new ArrayList<>(allPassed());

        BankingVerificationResponse response =
                BankingVerificationResponse.of(
                        mutable,
                        fingerprint(),
                        OBSERVED_AT,
                        OBSERVED_AT.plusSeconds(300)
                );

        mutable.clear();

        assertEquals(11, response.checks().size());
        assertEquals(response.checks(), response.toEvidence().checks());
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.checks().clear()
        );
    }

    @Test
    void rejectsIncompleteChecksAndInvalidValidity() {
        List<VerificationCheck> incomplete = Arrays.stream(
                        VerificationCheckType.values()
                )
                .filter(type -> type != VerificationCheckType.REQUIRED_KYC_VERIFIED)
                .map(VerificationCheck::passed)
                .toList();

        assertThrows(
                CustomerVerificationDomainException.class,
                () -> BankingVerificationResponse.of(
                        incomplete,
                        fingerprint(),
                        OBSERVED_AT,
                        null
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> BankingVerificationResponse.of(
                        allPassed(),
                        fingerprint(),
                        OBSERVED_AT,
                        OBSERVED_AT.minusSeconds(1)
                )
        );
    }

    private static List<VerificationCheck> allPassed() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }

    private static VerificationEvidenceFingerprint fingerprint() {
        return VerificationEvidenceFingerprint.of(
                "v1:sha256:" + "f".repeat(64)
        );
    }
}
