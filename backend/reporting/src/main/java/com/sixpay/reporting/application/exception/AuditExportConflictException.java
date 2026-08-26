package com.sixpay.reporting.application.exception;

public final class AuditExportConflictException extends RuntimeException {
    public AuditExportConflictException(String message) {
        super(message);
    }
}
