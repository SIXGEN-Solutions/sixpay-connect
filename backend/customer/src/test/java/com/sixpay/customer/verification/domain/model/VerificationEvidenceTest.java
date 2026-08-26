package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationEvidenceTest {

    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T12:00:00Z");

    @Test
    void canonicalizesAndProtectsTheMandatoryCheckCollection() {
        List<VerificationCheck> reversed = Arrays.stream(
                        VerificationCheckType.values()
                )
                .map(VerificationCheck::passed)
                .sorted((left, right) -> Integer.compare(
                        right.type().ordinal(),
                        left.type().ordinal()
                ))
                .toList();

        VerificationEvidence evidence = VerificationEvidence.of(
                reversed,
                fingerprint(),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300)
        );

        assertEquals(
                List.of(VerificationCheckType.values()),
                evidence.checks().stream()
                        .map(VerificationCheck::type)
                        .toList()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> evidence.checks().add(
                        VerificationCheck.passed(
                                VerificationCheckType.CUSTOMER_EXISTS
                        )
                )
        );
    }

    @Test
    void rejectsValidityBeforeObservation() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> VerificationEvidence.of(
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
                "v1:sha256:" + "a".repeat(64)
        );
    }
}
