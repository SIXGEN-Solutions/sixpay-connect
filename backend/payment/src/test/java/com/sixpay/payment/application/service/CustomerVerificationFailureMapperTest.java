package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.RetryDisposition;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerVerificationFailureMapperTest {

    private final CustomerVerificationFailureMapper mapper =
            new CustomerVerificationFailureMapper();

    @Test
    void verifiedProducesNoFailure() {
        assertNull(mapper.from(
                response(
                        CustomerVerificationResponse.Outcome.VERIFIED,
                        allPassed()
                ),
                Instant.parse("2026-08-03T20:00:00Z")
        ));
    }

    @Test
    void rejectedProducesNonRetryableBusinessFailure() {
        ArrayList<CustomerVerificationResponse.Check> checks =
                new ArrayList<>(allPassed());
        checks.set(
                CustomerVerificationResponse.CheckType
                        .ACCOUNT_EXISTS.ordinal(),
                new CustomerVerificationResponse.Check(
                        CustomerVerificationResponse.CheckType.ACCOUNT_EXISTS,
                        CustomerVerificationResponse.CheckResult.FAIL,
                        "ACCOUNT_NOT_FOUND"
                )
        );

        var failure = mapper.from(
                response(
                        CustomerVerificationResponse.Outcome.REJECTED,
                        checks
                ),
                Instant.parse("2026-08-03T20:00:00Z")
        );

        assertEquals(
                FailureCategory.BUSINESS_REJECTION,
                failure.failureCategory()
        );
        assertEquals(
                RetryDisposition.NOT_RETRYABLE,
                failure.retryDisposition()
        );
        assertEquals(
                "ACCOUNT_NOT_FOUND",
                failure.failureCode().value()
        );
    }

    @Test
    void indeterminateProducesRecoverableTechnicalFailure() {
        ArrayList<CustomerVerificationResponse.Check> checks =
                new ArrayList<>(allPassed());
        checks.set(
                CustomerVerificationResponse.CheckType
                        .NIU_MATCHES.ordinal(),
                new CustomerVerificationResponse.Check(
                        CustomerVerificationResponse.CheckType.NIU_MATCHES,
                        CustomerVerificationResponse.CheckResult.UNKNOWN,
                        "TECHNICAL_RESULT_UNKNOWN"
                )
        );

        var failure = mapper.from(
                response(
                        CustomerVerificationResponse.Outcome.INDETERMINATE,
                        checks
                ),
                Instant.parse("2026-08-03T20:00:00Z")
        );

        assertEquals(
                FailureCategory.TECHNICAL_FAILURE,
                failure.failureCategory()
        );
        assertEquals(
                RetryDisposition.SAFE_RETRY,
                failure.retryDisposition()
        );
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
                outcome == CustomerVerificationResponse.Outcome.VERIFIED
                        ? "CUSTOMER-VERIFIED-001"
                        : null,
                outcome == CustomerVerificationResponse.Outcome.VERIFIED
                        ? "ACCOUNT-VERIFIED-001"
                        : null,
                Instant.parse("2026-08-03T19:59:59Z"),
                Instant.parse("2026-08-03T20:04:59Z"),
                Instant.parse("2026-08-03T20:00:00Z")
        );
    }

    private static List<CustomerVerificationResponse.Check> allPassed() {
        return Arrays.stream(
                        CustomerVerificationResponse.CheckType.values()
                )
                .map(type ->
                        new CustomerVerificationResponse.Check(
                                type,
                                CustomerVerificationResponse
                                        .CheckResult.PASS,
                                null
                        )
                )
                .toList();
    }
}
