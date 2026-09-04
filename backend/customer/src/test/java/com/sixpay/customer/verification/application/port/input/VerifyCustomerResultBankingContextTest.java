package com.sixpay.customer.verification.application.port.input;

import com.sixpay.customer.verification.application.port.output.*;
import com.sixpay.customer.verification.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerifyCustomerResultBankingContextTest {

    @Test
    void verifiedResultExposesCanonicalBankingContext() {
        Instant instant =
                Instant.parse("2026-08-30T20:01:00Z");

        List<VerificationCheck> checks =
                Arrays.stream(VerificationCheckType.values())
                        .map(VerificationCheck::passed)
                        .toList();

        VerifiedBankingIdentity identity =
                new VerifiedBankingIdentity(
                        "CUST-0001",
                        "000001",
                        "AMPLITUDE",
                        "M012345678901",
                        "Ada Lovelace",
                        "+237690000001",
                        "ada@example.test",
                        "COMPLETE",
                        List.of(),
                        instant,
                        instant
                );

        VerifiedBankingAccount account =
                new VerifiedBankingAccount(
                        "ACC-0001",
                        "CUST-0001",
                        "AMPLITUDE",
                        "****0001",
                        "XAF",
                        "CURRENT",
                        "ACTIVE",
                        List.of(),
                        instant
                );

        VerifyCustomerResult result = VerifyCustomerResult.of(
                new CustomerVerificationId(UUID.randomUUID()),
                VerificationOutcome.VERIFIED,
                checks,
                VerificationEvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                ),
                AccountBindingFingerprint.of(
                        "v1:" + "a".repeat(64)
                ),
                instant,
                instant.plusSeconds(300),
                instant.plusSeconds(1),
                "CUST-0001",
                "ACC-0001",
                identity,
                account
        );

        assertThat(result.customerReference())
                .isEqualTo("CUST-0001");
        assertThat(result.accountReference())
                .isEqualTo("ACC-0001");
        assertThat(result.identity().phoneNumber())
                .isEqualTo("+237690000001");
        assertThat(result.identity().email())
                .isEqualTo("ada@example.test");
    }
}
