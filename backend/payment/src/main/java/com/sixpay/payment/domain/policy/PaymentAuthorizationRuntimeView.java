package com.sixpay.payment.domain.policy;

/**
 * Independent current-state facts. Null means unavailable/unresolved, false means
 * explicitly not authorized, true means positively verified. No JWT or OTP stored.
 */
public record PaymentAuthorizationRuntimeView(
        Boolean subscriptionAuthorized,
        Boolean applicationAuthorized
) { }
