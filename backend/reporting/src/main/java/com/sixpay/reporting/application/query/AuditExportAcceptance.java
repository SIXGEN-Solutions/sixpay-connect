package com.sixpay.reporting.application.query;

public record AuditExportAcceptance(
        AuditExportJobDefinition job,
        boolean newlyCreated
) {
}
