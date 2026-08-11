package com.sixpay.security.domain.administration;

public enum SecurityAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    PASSWORD_RESET,
    ACCOUNT_LOCKED,
    OIDC_LOGIN_SUCCESS,
    OIDC_LOGIN_FAILURE,
    IDENTITY_LINKED,
    IDENTITY_UNLINKED,
    AUTH_METHOD_ENABLED,
    AUTH_METHOD_DISABLED,
    USER_DISABLED
}
