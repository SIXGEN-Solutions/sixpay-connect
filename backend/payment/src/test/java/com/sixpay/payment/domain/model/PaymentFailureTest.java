package com.sixpay.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentFailureTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-31T18:00:00Z");

    @Test
    void businessRejectionAcceptsOnlyApprovedDispositions() {
        PaymentFailure failure = failure(
                FailureCategory.BUSINESS_REJECTION,
                RetryDisposition.NOT_RETRYABLE
        );

        assertEquals(
                FailureCode.of("INSUFFICIENT_FUNDS"),
                failure.failureCode()
        );
        assertEquals(
                "Insufficient funds",
                failure.safeMessage()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> failure(
                        FailureCategory.BUSINESS_REJECTION,
                        RetryDisposition.SAFE_RETRY
                )
        );
    }

    @Test
    void uncertainOutcomeRequiresLookupOrOperatorAction() {
        PaymentFailure lookup = failure(
                FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED
        );

        assertEquals(
                RetryDisposition.AUTHORITATIVE_LOOKUP_REQUIRED,
                lookup.retryDisposition()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> failure(
                        FailureCategory.UNCERTAIN_EXTERNAL_OUTCOME,
                        RetryDisposition.SAFE_RETRY
                )
        );
    }

    @Test
    void technicalAndTreasuryFailuresFollowTheirMatrices() {
        failure(
                FailureCategory.TECHNICAL_FAILURE,
                RetryDisposition.SAFE_RETRY
        );
        failure(
                FailureCategory.TECHNICAL_FAILURE,
                RetryDisposition.RECOVERY_EVENT_REQUIRED
        );
        failure(
                FailureCategory.TREASURY_RECONCILIATION_FAILURE,
                RetryDisposition.RECOVERY_EVENT_REQUIRED
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> failure(
                        FailureCategory.TREASURY_RECONCILIATION_FAILURE,
                        RetryDisposition.SAFE_RETRY
                )
        );
    }

    @Test
    void safeMessageIsTrimmedAndRejectsObviousSecretsOrDiagnostics() {
        PaymentFailure failure = new PaymentFailure(
                FailureCode.of("ACCOUNT_BLOCKED"),
                FailureCategory.BUSINESS_REJECTION,
                FailureStage.BANKING_VERIFICATION,
                RetryDisposition.NOT_RETRYABLE,
                "  Account blocked by bank policy  ",
                OCCURRED_AT,
                ExternalSystem.AMPLITUDE
        );

        assertEquals(
                "Account blocked by bank policy",
                failure.safeMessage()
        );
        assertEquals(
                ExternalSystem.AMPLITUDE,
                failure.externalSystemOptional().orElseThrow()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentFailure(
                        FailureCode.of("TECHNICAL_FAILURE"),
                        FailureCategory.TECHNICAL_FAILURE,
                        FailureStage.INTAKE,
                        RetryDisposition.SAFE_RETRY,
                        "Bearer secret-value",
                        OCCURRED_AT,
                        ExternalSystem.SIXPAY
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentFailure(
                        FailureCode.of("TECHNICAL_FAILURE"),
                        FailureCategory.TECHNICAL_FAILURE,
                        FailureStage.INTAKE,
                        RetryDisposition.SAFE_RETRY,
                        "at com.example.Client.call(Client.java:42)",
                        OCCURRED_AT,
                        ExternalSystem.SIXPAY
                )
        );
    }

    @Test
    void externalSystemIsOptional() {
        PaymentFailure failure = new PaymentFailure(
                FailureCode.of("PAYMENT_REFERENCE_CONFLICT"),
                FailureCategory.INTEGRATION_CONFLICT,
                FailureStage.INTAKE,
                RetryDisposition.NOT_RETRYABLE,
                "Payment reference conflicts with the original request",
                OCCURRED_AT,
                null
        );

        assertFalse(failure.externalSystemOptional().isPresent());
    }

    @Test
    void failureCodeFormatIsStableAndUppercase() {
        assertEquals(
                "TFJ_MATCH_AMBIGUOUS",
                FailureCode.of("TFJ_MATCH_AMBIGUOUS").value()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> FailureCode.of("dynamic-code")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FailureCode.of("AB")
        );
    }

    private static PaymentFailure failure(
            FailureCategory category,
            RetryDisposition disposition
    ) {
        return new PaymentFailure(
                FailureCode.of("INSUFFICIENT_FUNDS"),
                category,
                FailureStage.FUNDS_CONTROL,
                disposition,
                "Insufficient funds",
                OCCURRED_AT,
                ExternalSystem.AMPLITUDE
        );
    }
}
