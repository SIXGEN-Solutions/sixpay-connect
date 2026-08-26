package com.sixpay.customer.verification.application.port.input;

import com.sixpay.customer.verification.domain.exception.CustomerVerificationDomainException;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerifyCustomerResultTest {

    private static final Instant OBSERVED = Instant.parse("2026-08-03T18:00:01Z");
    private static final Instant COMPLETED = Instant.parse("2026-08-03T18:00:02Z");

    @Test
    void createsDefensiveCanonicalResult() {
        ArrayList<VerificationCheck> mutable = new ArrayList<>(allPassed());
        VerifyCustomerResult result = result(VerificationOutcome.VERIFIED, mutable);
        mutable.clear();

        assertEquals(11, result.checks().size());
        assertThrows(UnsupportedOperationException.class, () -> result.checks().clear());
    }

    @Test
    void rejectsOutcomeContradictingChecks() {
        assertThrows(
                CustomerVerificationDomainException.class,
                () -> result(VerificationOutcome.REJECTED, allPassed())
        );
    }

    @Test
    void supportsRejectedAndIndeterminate() {
        ArrayList<VerificationCheck> failed = new ArrayList<>(allPassed());
        failed.set(
                VerificationCheckType.ACCOUNT_EXISTS.ordinal(),
                VerificationCheck.failed(
                        VerificationCheckType.ACCOUNT_EXISTS,
                        VerificationFailureCode.ACCOUNT_NOT_FOUND
                )
        );
        assertEquals(VerificationOutcome.REJECTED, result(VerificationOutcome.REJECTED, failed).outcome());

        ArrayList<VerificationCheck> unknown = new ArrayList<>(allPassed());
        unknown.set(
                VerificationCheckType.NIU_MATCHES.ordinal(),
                VerificationCheck.unknown(
                        VerificationCheckType.NIU_MATCHES,
                        VerificationFailureCode.TECHNICAL_RESULT_UNKNOWN
                )
        );
        assertEquals(
                VerificationOutcome.INDETERMINATE,
                result(VerificationOutcome.INDETERMINATE, unknown).outcome()
        );
    }

    private static VerifyCustomerResult result(
            VerificationOutcome outcome,
            List<VerificationCheck> checks
    ) {
        return VerifyCustomerResult.of(
                new CustomerVerificationId(UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a")),
                outcome,
                checks,
                VerificationEvidenceFingerprint.of("v1:sha256:" + "b".repeat(64)),
                AccountBindingFingerprint.of("v1:" + "a".repeat(64)),
                OBSERVED,
                OBSERVED.plusSeconds(300),
                COMPLETED
        );
    }

    private static List<VerificationCheck> allPassed() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }
}
