package com.sixpay.integration.resilience;

import com.sixpay.integration.error.ExternalIntegrationException;

@FunctionalInterface
public interface RetryDecider {
    boolean shouldRetry(
            IntegrationOperationType operationType,
            ExternalIntegrationException failure,
            int completedAttempts
    );
    static RetryDecider safeDefault() {
        return (type, failure, attempts) ->
                failure.failure().retryable()
                && !failure.failure().outcomeUnknown()
                && type != IntegrationOperationType.FINANCIAL_COMMAND;
    }
}
