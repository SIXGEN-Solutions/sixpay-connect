package com.sixpay.payment.domain.policy;

/**
 * Classification of the authoritative source used by an authorization control.
 */
public enum AuthorizationSourceKind {
    PAYMENT_STATE,
    TRUSTED_INTAKE_ATTESTATION,
    OWNED_DOMAIN_RUNTIME_SOURCE,
    REQUIRES_RUNTIME_SOURCE
}
