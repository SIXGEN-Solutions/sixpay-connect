package com.sixpay.integration.error;

import java.util.Objects;

public class ExternalIntegrationException extends RuntimeException {
    private final ExternalFailure failure;
    public ExternalIntegrationException(ExternalFailure failure, Throwable cause) {
        super(Objects.requireNonNull(failure, "failure is required").safeMessage(), cause);
        this.failure = failure;
    }
    public ExternalIntegrationException(ExternalFailure failure) { this(failure, null); }
    public ExternalFailure failure() { return failure; }
}
