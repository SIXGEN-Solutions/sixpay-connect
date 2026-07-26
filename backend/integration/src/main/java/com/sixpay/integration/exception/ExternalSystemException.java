package com.sixpay.integration.exception;

import com.sixpay.common.validation.Preconditions;
import com.sixpay.integration.system.ExternalSystem;

/**
 * Represents a technical or remote error encountered while
 * communicating with an external system.
 */
public class ExternalSystemException extends RuntimeException {

    private final ExternalSystem externalSystem;
    private final IntegrationErrorType errorType;
    private final boolean retryable;

    public ExternalSystemException(
            ExternalSystem externalSystem,
            IntegrationErrorType errorType,
            boolean retryable,
            String message
    ) {
        super(
                Preconditions.requireNonBlank(
                        message,
                        "Integration error message must not be blank"
                )
        );

        this.externalSystem = Preconditions.requireNonNull(
                externalSystem,
                "External system must not be null"
        );

        this.errorType = Preconditions.requireNonNull(
                errorType,
                "Integration error type must not be null"
        );

        this.retryable = retryable;
    }

    public ExternalSystemException(
            ExternalSystem externalSystem,
            IntegrationErrorType errorType,
            boolean retryable,
            String message,
            Throwable cause
    ) {
        super(
                Preconditions.requireNonBlank(
                        message,
                        "Integration error message must not be blank"
                ),
                cause
        );

        this.externalSystem = Preconditions.requireNonNull(
                externalSystem,
                "External system must not be null"
        );

        this.errorType = Preconditions.requireNonNull(
                errorType,
                "Integration error type must not be null"
        );

        this.retryable = retryable;
    }

    public ExternalSystem externalSystem() {
        return externalSystem;
    }

    public IntegrationErrorType errorType() {
        return errorType;
    }

    public boolean retryable() {
        return retryable;
    }
}