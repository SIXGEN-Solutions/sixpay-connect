package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Current relevant structured Payment failure.
 *
 * @param failureCode stable safe code
 * @param failureCategory failure classification
 * @param failureStage lifecycle stage
 * @param retryDisposition approved recovery disposition
 * @param safeMessage authorized safe explanation
 * @param occurredAt occurrence instant
 * @param externalSystem optional source or reporter
 */
public record PaymentFailure(
        FailureCode failureCode,
        FailureCategory failureCategory,
        FailureStage failureStage,
        RetryDisposition retryDisposition,
        String safeMessage,
        Instant occurredAt,
        ExternalSystem externalSystem
) implements ValueObject {

    public PaymentFailure {
        failureCode = PaymentValueObjectRules.requireNonNull(
                failureCode,
                "Failure code"
        );
        failureCategory =
                PaymentValueObjectRules.requireNonNull(
                        failureCategory,
                        "Failure category"
                );
        failureStage = PaymentValueObjectRules.requireNonNull(
                failureStage,
                "Failure stage"
        );
        retryDisposition =
                PaymentValueObjectRules.requireNonNull(
                        retryDisposition,
                        "Retry disposition"
                );
        safeMessage =
                PaymentValueObjectRules.requireSafeMessage(
                        safeMessage
                );
        occurredAt = PaymentValueObjectRules.requireNonNull(
                occurredAt,
                "Failure occurrence instant"
        );

        Set<RetryDisposition> allowed =
                allowedDispositions(failureCategory);

        if (!allowed.contains(retryDisposition)) {
            throw new IllegalArgumentException(
                    "Retry disposition " + retryDisposition
                            + " is not valid for failure category "
                            + failureCategory
            );
        }
    }

    public Optional<ExternalSystem> externalSystemOptional() {
        return Optional.ofNullable(externalSystem);
    }

    private static Set<RetryDisposition> allowedDispositions(
            FailureCategory category
    ) {
        return switch (category) {
            case BUSINESS_REJECTION, SECURITY_REJECTION ->
                    EnumSet.of(
                            RetryDisposition.NOT_RETRYABLE,
                            RetryDisposition.OPERATOR_ACTION_REQUIRED
                    );
            case TECHNICAL_FAILURE ->
                    EnumSet.of(
                            RetryDisposition.SAFE_RETRY,
                            RetryDisposition.RECOVERY_EVENT_REQUIRED,
                            RetryDisposition.OPERATOR_ACTION_REQUIRED
                    );
            case UNCERTAIN_EXTERNAL_OUTCOME ->
                    EnumSet.of(
                            RetryDisposition
                                    .AUTHORITATIVE_LOOKUP_REQUIRED,
                            RetryDisposition.OPERATOR_ACTION_REQUIRED
                    );
            case INTEGRATION_CONFLICT ->
                    EnumSet.of(
                            RetryDisposition.NOT_RETRYABLE,
                            RetryDisposition.OPERATOR_ACTION_REQUIRED
                    );
            case TREASURY_RECONCILIATION_FAILURE ->
                    EnumSet.of(
                            RetryDisposition.RECOVERY_EVENT_REQUIRED,
                            RetryDisposition.OPERATOR_ACTION_REQUIRED
                    );
        };
    }
}
