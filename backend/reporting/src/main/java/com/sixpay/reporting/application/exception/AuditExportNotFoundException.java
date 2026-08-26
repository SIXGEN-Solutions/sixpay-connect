package com.sixpay.reporting.application.exception;

public final class AuditExportNotFoundException extends RuntimeException {
    public AuditExportNotFoundException(String message) {
        super(message);
    }
}
