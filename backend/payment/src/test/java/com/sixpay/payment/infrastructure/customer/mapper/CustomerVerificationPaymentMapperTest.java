package com.sixpay.payment.infrastructure.customer.mapper;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.evidence.BankingVerificationCheckType;
import com.sixpay.payment.domain.model.evidence.BankingVerificationOutcome;
import com.sixpay.payment.domain.model.evidence.EvidenceCheckResult;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerVerificationPaymentMapperTest {

    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";

    private static final Instant OBSERVED_AT =
            Instant.parse("2026-08-03T18:30:01Z");

    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-03T18:30:02Z");

    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-08-03T18:30:03Z");

    private final CustomerVerificationPaymentMapper mapper =
            new CustomerVerificationPaymentMapper();

    @Test
    void mapsVerifiedResponseToCanonicalPaymentSnapshot() {
        var response = response(
                CustomerVerificationResponse.Outcome.VERIFIED,
                allPassed()
        );

        var snapshot = mapper.toSnapshot(
                response,
                CorrelationId.of(CORRELATION_ID),
                ACCEPTED_AT
        );

        assertEquals(
                response.verificationId(),
                snapshot.verificationId().value()
        );

        assertEquals(
                BankingVerificationOutcome.VERIFIED,
                snapshot.outcome()
        );

        assertEquals(
                response.accountBindingFingerprint(),
                snapshot.accountBindingFingerprint()
        );

        assertEquals(
                11,
                snapshot.checks().size()
        );

        assertEquals(
                ExternalSystem.AMPLITUDE,
                snapshot.metadata().sourceSystem()
        );

        assertEquals(
                EvidenceObservationChannel.DIRECT_RESPONSE,
                snapshot.metadata().observationChannel()
        );

        assertEquals(
                CORRELATION_ID,
                snapshot.metadata().correlationId().value()
        );

        assertEquals(
                response.evidenceFingerprint(),
                snapshot.metadata()
                        .evidenceFingerprint()
                        .value()
        );

        assertEquals(
                OBSERVED_AT,
                snapshot.metadata().observedAt()
        );

        assertEquals(
                ACCEPTED_AT,
                snapshot.metadata().acceptedAt()
        );
    }

    @Test
    void mapsRejectedCheckAndFailureCode() {
        ArrayList<CustomerVerificationResponse.Check> checks =
                new ArrayList<>(allPassed());

        checks.set(
                CustomerVerificationResponse.CheckType
                        .ACCOUNT_EXISTS
                        .ordinal(),
                new CustomerVerificationResponse.Check(
                        CustomerVerificationResponse.CheckType
                                .ACCOUNT_EXISTS,
                        CustomerVerificationResponse.CheckResult.FAIL,
                        "ACCOUNT_NOT_FOUND"
                )
        );

        var snapshot = mapper.toSnapshot(
                response(
                        CustomerVerificationResponse.Outcome.REJECTED,
                        checks
                ),
                CorrelationId.of(CORRELATION_ID),
                ACCEPTED_AT
        );

        var accountExists = snapshot.checks()
                .stream()
                .filter(check ->
                        check.type()
                                == BankingVerificationCheckType
                                .ACCOUNT_EXISTS
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                EvidenceCheckResult.FAIL,
                accountExists.result()
        );

        assertEquals(
                "ACCOUNT_NOT_FOUND",
                accountExists.reasonCodeOptional()
                        .orElseThrow()
                        .value()
        );

        assertEquals(
                OBSERVED_AT,
                accountExists.checkedAt()
        );
    }

    @Test
    void mapsIndeterminateUnknownCheck() {
        ArrayList<CustomerVerificationResponse.Check> checks =
                new ArrayList<>(allPassed());

        checks.set(
                CustomerVerificationResponse.CheckType
                        .NIU_MATCHES
                        .ordinal(),
                new CustomerVerificationResponse.Check(
                        CustomerVerificationResponse.CheckType
                                .NIU_MATCHES,
                        CustomerVerificationResponse.CheckResult.UNKNOWN,
                        "TECHNICAL_RESULT_UNKNOWN"
                )
        );

        var snapshot = mapper.toSnapshot(
                response(
                        CustomerVerificationResponse
                                .Outcome
                                .INDETERMINATE,
                        checks
                ),
                CorrelationId.of(CORRELATION_ID),
                ACCEPTED_AT
        );

        assertEquals(
                BankingVerificationOutcome.INDETERMINATE,
                snapshot.outcome()
        );

        var niu = snapshot.checks()
                .stream()
                .filter(check ->
                        check.type()
                                == BankingVerificationCheckType
                                .NIU_MATCHES
                )
                .findFirst()
                .orElseThrow();

        assertEquals(
                EvidenceCheckResult.UNKNOWN,
                niu.result()
        );

        assertEquals(
                "TECHNICAL_RESULT_UNKNOWN",
                niu.reasonCodeOptional()
                        .orElseThrow()
                        .value()
        );
    }

    @Test
    void mapsEveryCheckTypeExplicitly() {
        for (CustomerVerificationResponse.CheckType source
                : CustomerVerificationResponse.CheckType.values()) {

            BankingVerificationCheckType mapped =
                    CustomerVerificationPaymentMapper
                            .mapCheckType(source);

            assertEquals(
                    source.name(),
                    mapped.name()
            );
        }

        assertEquals(
                11,
                CustomerVerificationResponse
                        .CheckType
                        .values()
                        .length
        );

        assertEquals(
                11,
                BankingVerificationCheckType
                        .values()
                        .length
        );
    }

    @Test
    void mapsEveryOutcomeAndResultExplicitly() {
        assertEquals(
                BankingVerificationOutcome.VERIFIED,
                CustomerVerificationPaymentMapper.mapOutcome(
                        CustomerVerificationResponse
                                .Outcome
                                .VERIFIED
                )
        );

        assertEquals(
                BankingVerificationOutcome.REJECTED,
                CustomerVerificationPaymentMapper.mapOutcome(
                        CustomerVerificationResponse
                                .Outcome
                                .REJECTED
                )
        );

        assertEquals(
                BankingVerificationOutcome.INDETERMINATE,
                CustomerVerificationPaymentMapper.mapOutcome(
                        CustomerVerificationResponse
                                .Outcome
                                .INDETERMINATE
                )
        );

        assertEquals(
                EvidenceCheckResult.PASS,
                CustomerVerificationPaymentMapper.mapCheckResult(
                        CustomerVerificationResponse
                                .CheckResult
                                .PASS
                )
        );

        assertEquals(
                EvidenceCheckResult.FAIL,
                CustomerVerificationPaymentMapper.mapCheckResult(
                        CustomerVerificationResponse
                                .CheckResult
                                .FAIL
                )
        );

        assertEquals(
                EvidenceCheckResult.UNKNOWN,
                CustomerVerificationPaymentMapper.mapCheckResult(
                        CustomerVerificationResponse
                                .CheckResult
                                .UNKNOWN
                )
        );
    }

    @Test
    void passCheckCarriesNoFailureCode() {
        var mapped = CustomerVerificationPaymentMapper.mapCheck(
                new CustomerVerificationResponse.Check(
                        CustomerVerificationResponse.CheckType
                                .CUSTOMER_EXISTS,
                        CustomerVerificationResponse.CheckResult.PASS,
                        null
                ),
                OBSERVED_AT
        );

        assertNull(mapped.reasonCode());
    }

    private static CustomerVerificationResponse response(
            CustomerVerificationResponse.Outcome outcome,
            List<CustomerVerificationResponse.Check> checks
    ) {
        return new CustomerVerificationResponse(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                outcome,
                checks,
                "v1:sha256:" + "b".repeat(64),
                "v1:" + "a".repeat(64),
                OBSERVED_AT,
                OBSERVED_AT.plusSeconds(300),
                COMPLETED_AT
        );
    }

    private static List<CustomerVerificationResponse.Check>
    allPassed() {

        return Arrays.stream(
                        CustomerVerificationResponse
                                .CheckType
                                .values()
                )
                .map(type ->
                        new CustomerVerificationResponse.Check(
                                type,
                                CustomerVerificationResponse
                                        .CheckResult
                                        .PASS,
                                null
                        )
                )
                .toList();
    }
}