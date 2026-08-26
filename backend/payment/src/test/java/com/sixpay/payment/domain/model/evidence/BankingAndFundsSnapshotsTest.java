package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankingAndFundsSnapshotsTest {

    @Test
    void verifiedBankingEvidenceContainsOnlyPassAndCanonicalOrder() {
        BankingVerificationSnapshot snapshot =
                new BankingVerificationSnapshot(
                        new BankingVerificationId(UUID.randomUUID()),
                        BankingVerificationOutcome.VERIFIED,
                        accountFingerprint(),
                        List.of(
                                bankingCheck(
                                        BankingVerificationCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS
                                ),
                                bankingCheck(
                                        BankingVerificationCheckType.CUSTOMER_EXISTS,
                                        EvidenceCheckResult.PASS
                                )
                        ),
                        amplitudeMetadata()
                );

        assertEquals(
                BankingVerificationCheckType.CUSTOMER_EXISTS,
                snapshot.checks().getFirst().type()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationSnapshot(
                        new BankingVerificationId(UUID.randomUUID()),
                        BankingVerificationOutcome.VERIFIED,
                        accountFingerprint(),
                        List.of(
                                bankingCheck(
                                        BankingVerificationCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.UNKNOWN
                                )
                        ),
                        amplitudeMetadata()
                )
        );
    }

    @Test
    void rejectedAndIndeterminateBankingResultsAreDistinct() {
        new BankingVerificationSnapshot(
                new BankingVerificationId(UUID.randomUUID()),
                BankingVerificationOutcome.REJECTED,
                accountFingerprint(),
                List.of(
                        new BankingVerificationCheckEvidence(
                                BankingVerificationCheckType.ACCOUNT_IS_ACTIVE,
                                EvidenceCheckResult.FAIL,
                                FailureCode.of("ACCOUNT_BLOCKED"),
                                null
                        )
                ),
                amplitudeMetadata()
        );

        new BankingVerificationSnapshot(
                new BankingVerificationId(UUID.randomUUID()),
                BankingVerificationOutcome.INDETERMINATE,
                accountFingerprint(),
                List.of(
                        bankingCheck(
                                BankingVerificationCheckType.REQUIRED_KYC_VERIFIED,
                                EvidenceCheckResult.UNKNOWN
                        )
                ),
                amplitudeMetadata()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationSnapshot(
                        new BankingVerificationId(UUID.randomUUID()),
                        BankingVerificationOutcome.INDETERMINATE,
                        accountFingerprint(),
                        List.of(
                                new BankingVerificationCheckEvidence(
                                        BankingVerificationCheckType.ACCOUNT_NOT_BLOCKED,
                                        EvidenceCheckResult.FAIL,
                                        FailureCode.of("ACCOUNT_BLOCKED"),
                                        null
                                )
                        ),
                        amplitudeMetadata()
                )
        );
    }

    @Test
    void verifiedFundsEvidenceIsPositiveFreshAndCanonical() {
        FundsControlSnapshot snapshot =
                new FundsControlSnapshot(
                        new FundsVerificationReference("FUNDS-RESULT-0001"),
                        FundsControlOutcome.VERIFIED,
                        Money.of(new BigDecimal("1000"), "XAF"),
                        accountFingerprint(),
                        List.of(
                                fundsCheck(
                                        FundsControlCheckType.AVAILABLE_FUNDS_SUFFICIENT,
                                        EvidenceCheckResult.PASS
                                ),
                                fundsCheck(
                                        FundsControlCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS
                                )
                        ),
                        Instant.parse("2026-07-31T10:10:00Z"),
                        amplitudeMetadata()
                );

        assertEquals(
                FundsControlCheckType.ACCOUNT_EXISTS,
                snapshot.checks().getFirst().type()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FundsControlSnapshot(
                        new FundsVerificationReference("FUNDS-RESULT-0002"),
                        FundsControlOutcome.VERIFIED,
                        Money.of(BigDecimal.ZERO, "XAF"),
                        accountFingerprint(),
                        List.of(
                                fundsCheck(
                                        FundsControlCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS
                                )
                        ),
                        Instant.parse("2026-07-31T10:10:00Z"),
                        amplitudeMetadata()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FundsControlSnapshot(
                        new FundsVerificationReference("FUNDS-RESULT-0003"),
                        FundsControlOutcome.VERIFIED,
                        Money.of(new BigDecimal("100"), "XAF"),
                        accountFingerprint(),
                        List.of(
                                fundsCheck(
                                        FundsControlCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS
                                )
                        ),
                        Instant.parse("2026-07-31T10:00:30Z"),
                        amplitudeMetadata()
                )
        );
    }

    @Test
    void fundsOutcomeMatrixRejectsContradictions() {
        new FundsControlSnapshot(
                new FundsVerificationReference("FUNDS-RESULT-0004"),
                FundsControlOutcome.REJECTED,
                Money.of(new BigDecimal("100"), "XAF"),
                accountFingerprint(),
                List.of(
                        new FundsControlCheckEvidence(
                                FundsControlCheckType.AVAILABLE_FUNDS_SUFFICIENT,
                                EvidenceCheckResult.FAIL,
                                FailureCode.of("INSUFFICIENT_FUNDS"),
                                Instant.parse("2026-07-31T10:00:00Z")
                        )
                ),
                Instant.parse("2026-07-31T10:10:00Z"),
                amplitudeMetadata()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FundsControlSnapshot(
                        new FundsVerificationReference("FUNDS-RESULT-0005"),
                        FundsControlOutcome.REJECTED,
                        Money.of(new BigDecimal("100"), "XAF"),
                        accountFingerprint(),
                        List.of(
                                fundsCheck(
                                        FundsControlCheckType.ACCOUNT_EXISTS,
                                        EvidenceCheckResult.PASS
                                )
                        ),
                        Instant.parse("2026-07-31T10:10:00Z"),
                        amplitudeMetadata()
                )
        );
    }

    private static BankingVerificationCheckEvidence bankingCheck(
            BankingVerificationCheckType type,
            EvidenceCheckResult result
    ) {
        return new BankingVerificationCheckEvidence(type, result, null, null);
    }

    private static FundsControlCheckEvidence fundsCheck(
            FundsControlCheckType type,
            EvidenceCheckResult result
    ) {
        return new FundsControlCheckEvidence(
                type,
                result,
                null,
                Instant.parse("2026-07-31T10:00:00Z")
        );
    }

    private static String accountFingerprint() {
        return "v1:" + "a".repeat(64);
    }

    private static EvidenceMetadata amplitudeMetadata() {
        return EvidenceMetadataTest.metadata(
                ExternalSystem.AMPLITUDE,
                EvidenceObservationChannel.DIRECT_RESPONSE
        );
    }
}
