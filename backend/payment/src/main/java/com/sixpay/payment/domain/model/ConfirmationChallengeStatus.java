package com.sixpay.payment.domain.model;

/**
 * Approved Payment-confirmation challenge statuses.
 *
 * <p>The vocabulary is contract-owned and must remain aligned with the
 * approved Payment Confirmation contracts at the external integration boundaries.</p>
 */
public enum ConfirmationChallengeStatus {
    ACTIVE,
    VERIFIED,
    EXPIRED,
    LOCKED,
    REPLACED,
    REVOKED
}
