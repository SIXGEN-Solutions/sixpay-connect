package com.sixpay.security.authorization;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Canonical SIXPAY business permissions assignable to a SIXPAY user.
 *
 * <p>Persisted values deliberately exclude Spring Security's {@code SCOPE_}
 * prefix. The prefix is added only when permissions are exposed as Spring
 * authorities.</p>
 */
public enum SixpayPermission {

    OBSERVED_CUSTOMER_READ("observed-customer.read"),

    CUSTOMER_READ("customer.read"),
    CUSTOMER_CREATE("customer.create"),
    CUSTOMER_UPDATE("customer.update"),
    CUSTOMER_SUSPEND("customer.suspend"),
    CUSTOMER_AUDIT_READ("customer.audit.read"),

    SUBSCRIPTION_READ("subscription.read"),
    SUBSCRIPTION_CREATE("subscription.create"),
    SUBSCRIPTION_UPDATE("subscription.update"),
    SUBSCRIPTION_SUSPEND("subscription.suspend"),
    SUBSCRIPTION_CLOSE("subscription.close"),

    PAYMENT_READ("payment.read"),
    PAYMENT_WRITE("payment.write"),
    PAYMENT_AUDIT("payment.audit"),
    PAYMENT_RECONCILE("payment.reconcile"),
    PAYMENT_REVERSE("payment.reverse"),

    PAYMENT_AUDIT_READ("payment.audit.read"),
    PAYMENT_AUDIT_EXPORT("payment.audit.export");

    private static final String SCOPE_PREFIX = "SCOPE_";

    private final String value;

    SixpayPermission(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String authority() {
        return SCOPE_PREFIX + value;
    }

    public static SixpayPermission fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(
                    "SIXPAY permission must not be blank"
            );
        }

        String canonical = rawValue.trim();
        if (canonical.regionMatches(
                true,
                0,
                SCOPE_PREFIX,
                0,
                SCOPE_PREFIX.length()
        )) {
            canonical = canonical.substring(SCOPE_PREFIX.length());
        }

        String expected = canonical.toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(permission ->
                        permission.value.equalsIgnoreCase(expected)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unknown SIXPAY permission: " + rawValue
                        )
                );
    }

    public static Set<String> valuesAsSet() {
        return Arrays.stream(values())
                .map(SixpayPermission::value)
                .collect(Collectors.toUnmodifiableSet());
    }
}
