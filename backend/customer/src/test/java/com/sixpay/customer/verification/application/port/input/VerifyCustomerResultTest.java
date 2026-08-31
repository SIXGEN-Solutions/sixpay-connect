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
        if (outcome != VerificationOutcome.VERIFIED) {
            return VerifyCustomerResult.of(
                    new CustomerVerificationId(UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a")),
                    outcome, checks,
                    VerificationEvidenceFingerprint.of("v1:sha256:" + "b".repeat(64)),
                    AccountBindingFingerprint.of("v1:" + "a".repeat(64)),
                    OBSERVED, OBSERVED.plusSeconds(300), COMPLETED,
                    null, null, null, null
            );
        }
        return VerifyCustomerResult.of(
                new CustomerVerificationId(UUID.fromString("7ed75090-8af7-4dfa-9b62-8e4dca73501a")),
                outcome, checks,
                VerificationEvidenceFingerprint.of("v1:sha256:" + "b".repeat(64)),
                AccountBindingFingerprint.of("v1:" + "a".repeat(64)),
                OBSERVED, OBSERVED.plusSeconds(300), COMPLETED,
                "CUST-0001", "ACC-0001",
                new com.sixpay.customer.verification.application.port.output.VerifiedBankingIdentity(
                        "CUST-0001", "000001", "AMPLITUDE", "M0123456", "Ada Lovelace",
                        "+237690000001", "ada@example.test", "COMPLETE", List.of(), OBSERVED, OBSERVED
                ),
                new com.sixpay.customer.verification.application.port.output.VerifiedBankingAccount(
                        "ACC-0001", "CUST-0001", "AMPLITUDE", "****0001",
                        "XAF", "CURRENT", "ACTIVE", List.of(), OBSERVED
                )
        );
    }

    private static List<VerificationCheck> allPassed() {
        return Arrays.stream(VerificationCheckType.values())
                .map(VerificationCheck::passed)
                .toList();
    }
}
