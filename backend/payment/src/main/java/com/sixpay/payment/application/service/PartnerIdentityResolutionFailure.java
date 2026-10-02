package com.sixpay.payment.application.service;

/**
 * Internal Payment classification for Partner identity alignment failures.
 *
 * <p>These values are not public HTTP/API error codes.</p>
 */
public enum PartnerIdentityResolutionFailure {
    AUTHENTICATED_PARTNER_NOT_RESOLVED,
    DECLARED_PARTNER_NOT_FOUND,
    PARTNER_IDENTITY_MISMATCH,
    PARTNER_NOT_AUTHORIZED
}
