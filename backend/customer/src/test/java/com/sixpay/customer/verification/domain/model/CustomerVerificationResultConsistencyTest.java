package com.sixpay.customer.verification.domain.model;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationResultConsistencyTest {

    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T13:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T13:00:01Z");

    @Test
    void verifiedWithFailIsImpossible() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationResult(
                        VerificationOutcome.VERIFIED,
                        evidenceWithFailure(),
                        COMPLETED_AT
                )
        );
    }

    @Test
    void rejectedWithoutFailIsImpossible() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationResult(
                        VerificationOutcome.REJECTED,
                        allPassedEvidence(),
                        COMPLETED_AT
                )
        );
    }

    @Test
    void indeterminateWithFailIsImpossible() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationResult(
                        VerificationOutcome.INDETERMINATE,
                        evidenceWithFailureAndUnknown(),
                        COMPLETED_AT
                )
        );
    }

    @Test
    void verifiedWithUnknownIsImpossible() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> new CustomerVerificationResult(
                        VerificationOutcome.VERIFIED,
                        evidenceWithUnknown(),
                        COMPLETED_AT
                )
        );
    }

    private static VerificationEvidence allPassedEvidence() {
        return evidence(allPassed());
    }

    private static VerificationEvidence evidenceWithFailure() {
        List<VerificationCheck> checks =
                new ArrayList<>(allPassed());

        checks.set(
                VerificationCheckType.ACCOUNT_NOT_BLOCKED.ordinal(),
                VerificationCheck.failed(
                        VerificationCheckType.ACCOUNT_NOT_BLOCKED,
                        VerificationFailureCode.ACCOUNT_BLOCKED
                )
        );

        return evidence(checks);
    }

    private static VerificationEvidence evidenceWithUnknown() {
        List<VerificationCheck> checks =
                new ArrayList<>(allPassed());

        checks.set(
                VerificationCheckType.ACCOUNT_EXISTS.ordinal(),
                VerificationCheck.unknown(
                        VerificationCheckType.ACCOUNT_EXISTS,
                        VerificationFailureCode.BANKING_RESPONSE_TIMEOUT
                )
        );

        return evidence(checks);
    }

    private static VerificationEvidence evidenceWithFailureAndUnknown() {
        List<VerificationCheck> checks =
                new ArrayList<>(allPassed());

        checks.set(
                VerificationCheckType.ACCOUNT_NOT_BLOCKED.ordinal(),
                VerificationCheck.failed(
                        VerificationCheckType.ACCOUNT_NOT_BLOCKED,
                        VerificationFailureCode.ACCOUNT_BLOCKED
                )
        );
        checks.set(
                VerificationCheckType.ACCOUNT_EXISTS.ordinal(),
                VerificationCheck.unknown(
                        VerificationCheckType.ACCOUNT_EXISTS,
                        VerificationFailureCode.BANKING_RESPONSE_TIMEOUT
                )
        );

        return evidence(checks);
    }

    private static VerificationEvidence evidence(
            List<VerificationCheck> checks
    ) {
        return VerificationEvidence.of(
                checks,
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "c".repeat(64)
                ),
                OBSERVED_AT,
                COMPLETED_AT.plusSeconds(60)
        );
    }

    private static List<VerificationCheck> allPassed() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }
}
