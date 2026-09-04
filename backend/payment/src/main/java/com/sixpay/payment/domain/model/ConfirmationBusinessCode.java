package com.sixpay.payment.domain.model;

/**
 * Approved Payment-confirmation functional result vocabulary.
 */
public enum ConfirmationBusinessCode {
    CHALLENGE_ACTIVE,
    OTP_VERIFIED,
    OTP_INVALID,
    CHALLENGE_EXPIRED,
    CHALLENGE_LOCKED,
    CHALLENGE_REPLACED,
    CHALLENGE_REVOKED,
    RESEND_NOT_ALLOWED,
    CONFIRMATION_NOT_AVAILABLE,
    DEPENDENCY_RESULT_UNKNOWN,
    DELIVERY_FAILED
}
