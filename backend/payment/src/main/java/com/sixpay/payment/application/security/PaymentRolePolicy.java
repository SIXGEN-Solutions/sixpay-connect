package com.sixpay.payment.application.security;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role-to-action policy for Payment.
 */
@Component
public final class PaymentRolePolicy {

    private static final Set<SixpayRole> INTERNAL_READ_ROLES =
            EnumSet.of(
                    SixpayRole.ADMIN,
                    SixpayRole.OPS,
                    SixpayRole.SUPPORT,
                    SixpayRole.MANAGER,
                    SixpayRole.AUDITOR,
                    SixpayRole.READ_ONLY
            );

    private static final Set<SixpayRole> OPERATE_ROLES =
            EnumSet.of(
                    SixpayRole.ADMIN,
                    SixpayRole.OPS,
                    SixpayRole.MANAGER
            );

    private static final Set<SixpayRole> AUDIT_ROLES =
            EnumSet.of(
                    SixpayRole.ADMIN,
                    SixpayRole.AUDITOR,
                    SixpayRole.MANAGER
            );

    public boolean permits(
            AuthenticatedUser user,
            PaymentAction action
    ) {
        Objects.requireNonNull(user, "Authenticated user");
        Objects.requireNonNull(action, "Payment action");

        return switch (action) {
            case SEARCH, READ ->
                    hasAnyRole(user, INTERNAL_READ_ROLES)
                            || user.hasRole(SixpayRole.PARTNER);
            case OPERATE ->
                    hasAnyRole(user, OPERATE_ROLES);
            case AUDIT ->
                    hasAnyRole(user, AUDIT_ROLES);
            case RECONCILE ->
                    user.hasRole(SixpayRole.ADMIN)
                            || user.hasRole(SixpayRole.OPS);
            case REVERSE ->
                    user.hasRole(SixpayRole.ADMIN)
                            || user.hasRole(SixpayRole.MANAGER);
        };
    }

    private static boolean hasAnyRole(
            AuthenticatedUser user,
            Set<SixpayRole> roles
    ) {
        return roles.stream().anyMatch(user::hasRole);
    }
}
