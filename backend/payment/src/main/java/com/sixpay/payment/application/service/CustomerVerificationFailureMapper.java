package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.CustomerVerificationResponse;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCategory;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.FailureStage;
import com.sixpay.payment.domain.model.PaymentFailure;
import com.sixpay.payment.domain.model.RetryDisposition;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * Converts Customer Verification conclusions into structured Payment failures.
 */
@Component
public final class CustomerVerificationFailureMapper {

    public PaymentFailure from(
            CustomerVerificationResponse response,
            Instant occurredAt
    ) {
        Objects.requireNonNull(response, "response is required");
        Objects.requireNonNull(occurredAt, "occurredAt is required");

        return switch (response.outcome()) {
            case VERIFIED -> null;
            case REJECTED -> new PaymentFailure(
                    FailureCode.of(firstFailureCode(
                            response,
                            "CUSTOMER_VERIFICATION_REJECTED"
                    )),
                    FailureCategory.BUSINESS_REJECTION,
                    FailureStage.BANKING_VERIFICATION,
                    RetryDisposition.NOT_RETRYABLE,
                    "Customer verification was rejected",
                    occurredAt,
                    ExternalSystem.AMPLITUDE
            );
            case INDETERMINATE -> new PaymentFailure(
                    FailureCode.of(firstFailureCode(
                            response,
                            "CUSTOMER_VERIFICATION_INDETERMINATE"
                    )),
                    FailureCategory.TECHNICAL_FAILURE,
                    FailureStage.BANKING_VERIFICATION,
                    RetryDisposition.SAFE_RETRY,
                    "Customer verification is indeterminate",
                    occurredAt,
                    ExternalSystem.AMPLITUDE
            );
        };
    }

    private static String firstFailureCode(
            CustomerVerificationResponse response,
            String fallback
    ) {
        return response.checks().stream()
                .filter(check ->
                        check.result()
                                != CustomerVerificationResponse
                                        .CheckResult.PASS
                )
                .map(CustomerVerificationResponse.Check::failureCode)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }
}
