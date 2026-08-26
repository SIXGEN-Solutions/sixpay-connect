package com.sixpay.reporting.domain.exception;

/**
 * Base exception for Reporting-domain invariant violations.
 */
public class ReportingDomainException extends RuntimeException {

    public ReportingDomainException(String message) {
        super(message);
    }

    public ReportingDomainException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
