package com.sixpay.payment.application.security;

/**
 * Payment authorities used by application policies.
 *
 * <p>Roles remain centralized input {@code SixpayRole}; this enum represents
 * permissions only.</p>
 */
public enum PaymentAuthority {
    READ("SCOPE_payment.read"),
    OPERATE("SCOPE_payment.write"),
    AUDIT("SCOPE_payment.audit"),
    RECONCILE("SCOPE_payment.reconcile"),
    REVERSE("SCOPE_payment.reverse");

    private final String authority;

    PaymentAuthority(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
