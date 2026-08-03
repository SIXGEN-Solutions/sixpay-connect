package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationResultTest {

    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T12:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T12:00:01Z");

    @Test
    void derivesCanonicalOutcomeFromEvidence() {
        CustomerVerificationResult result =
                CustomerVerificationResult.from(
                        verifiedEvidence(),
                        COMPLETED_AT
                );

        assertEquals(VerificationOutcome.VERIFIED, result.outcome());
    }

    @Test
    void rejectsOutcomeContradictingEvidence() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationResult(
                        VerificationOutcome.REJECTED,
                        verifiedEvidence(),
                        COMPLETED_AT
                )
        );
    }

    @Test
    void rejectsCompletionBeforeObservation() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> CustomerVerificationResult.from(
                        verifiedEvidence(),
                        OBSERVED_AT.minusSeconds(1)
                )
        );
    }

    private static VerificationEvidence verifiedEvidence() {
        return VerificationEvidence.of(
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList(),
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                ),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300)
        );
    }
}
