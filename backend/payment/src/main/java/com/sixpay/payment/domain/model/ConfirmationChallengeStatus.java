package com.sixpay.payment.domain.model;

/**
 * Approved Payment-confirmation challenge statuses.
 *
 * <p>The vocabulary is contract-owned and must remain aligned with the
 * approved Amplitude and TRESOR PAY Payment Confirmation contracts.</p>
 */
public enum ConfirmationChallengeStatus {
    ACTIVE,
    VERIFIED,
    EXPIRED,
    LOCKED,
    REPLACED,
    REVOKED
}
