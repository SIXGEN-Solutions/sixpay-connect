package com.sixpay.reporting.application.port.output;

/**
 * Marker for read-only normalized audit-evidence sources.
 *
 * <p>Reporting adapters must consume normalized evidence and must not load
 * aggregates owned by Payment, Customer, Accounting or Notification.</p>
 */
public interface AuditEvidenceReadPort {
}
