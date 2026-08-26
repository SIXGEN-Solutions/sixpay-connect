package com.sixpay.customer.observation.infrastructure.resilience;

/**
 * Closed and bounded classification used by the projection retry policy.
 */
public enum ObservedCustomerProjectionFailureType {
    OPTIMISTIC_LOCK(true),
    TRANSIENT_TRANSACTION(true),
    DEADLOCK(true),
    SERIALIZATION_FAILURE(true),
    TEMPORARY_CONNECTION(true),
    IDEMPOTENCE_RACE(true),
    INVALID_PAYLOAD(false),
    CONTRADICTORY_IDENTITY(false),
    UNKNOWN_STATUS(false),
    MISSING_REQUIRED_DATA(false),
    PERMANENT_CRYPTOGRAPHY(false),
    INCOMPATIBLE_CONTRACT(false),
    NON_RETRYABLE(false);

    private final boolean retryable;

    ObservedCustomerProjectionFailureType(
            boolean retryable
    ) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean idempotenceRace() {
        return this == IDEMPOTENCE_RACE;
    }
}
