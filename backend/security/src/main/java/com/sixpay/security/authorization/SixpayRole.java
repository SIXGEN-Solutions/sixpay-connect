package com.sixpay.security.authorization;

/**
 * Platform-wide security roles recognized by SIXPAY CONNECT.
 */
public enum SixpayRole {

    ADMIN,
    OPS,
    SUPPORT,
    MANAGER,
    AUDITOR,
    READ_ONLY,
    PARTNER;

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Returns the Spring Security authority associated with this role.
     *
     * @return Spring Security authority
     */
    public String authority() {
        return ROLE_PREFIX + name();
    }
}