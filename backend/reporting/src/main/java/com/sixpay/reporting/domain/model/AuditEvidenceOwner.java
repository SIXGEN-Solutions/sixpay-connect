package com.sixpay.reporting.domain.model;

/**
 * Contract-declared owners of evidence that Reporting may normalize.
 */
public enum AuditEvidenceOwner {
    PAYMENT,
    CUSTOMER,
    ACCOUNTING,
    NOTIFICATION,
    INTEGRATION
}
