package com.sixpay.payment.application.security;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Fail-closed Partner ownership policy.
 */
@Component
public final class PaymentPartnerIsolationPolicy {

    public boolean isPartner(AuthenticatedUser user) {
        return Objects.requireNonNull(user, "Authenticated user")
                .hasRole(SixpayRole.PARTNER);
    }

    public PaymentVisibilityScope visibilityFor(
            AuthenticatedUser user
    ) {
        Objects.requireNonNull(user, "Authenticated user");

        return isPartner(user)
                ? new PaymentVisibilityScope.Partner(
                        user.subject()
                )
                : new PaymentVisibilityScope.Internal();
    }

    public boolean canAccess(
            AuthenticatedUser user,
            PaymentObjectAccessDescriptor object
    ) {
        Objects.requireNonNull(user, "Authenticated user");
        Objects.requireNonNull(object, "Payment access object");

        if (!isPartner(user)) {
            return true;
        }

        return object.partnerSubjectOptional()
                .map(user.subject()::equals)
                .orElse(false);
    }
}
