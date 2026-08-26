package com.sixpay.payment.application.security;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Central Payment authorization policy.
 */
@Component("paymentAccessPolicy")
public final class PaymentAccessPolicy {

    private final CurrentUserProvider currentUserProvider;
    private final PaymentRolePolicy rolePolicy;
    private final PaymentPartnerIsolationPolicy partnerIsolation;

    public PaymentAccessPolicy(
            CurrentUserProvider currentUserProvider,
            PaymentRolePolicy rolePolicy,
            PaymentPartnerIsolationPolicy partnerIsolation
    ) {
        this.currentUserProvider = Objects.requireNonNull(
                currentUserProvider,
                "Current user provider"
        );
        this.rolePolicy = Objects.requireNonNull(
                rolePolicy,
                "Payment role policy"
        );
        this.partnerIsolation = Objects.requireNonNull(
                partnerIsolation,
                "Partner isolation policy"
        );
    }

    public boolean canSearch() {
        return can(PaymentAction.SEARCH);
    }

    public boolean canRead() {
        return can(PaymentAction.READ);
    }

    public boolean canOperate() {
        return can(PaymentAction.OPERATE);
    }

    public boolean canAudit() {
        return can(PaymentAction.AUDIT);
    }

    public boolean canReconcile() {
        return can(PaymentAction.RECONCILE);
    }

    public boolean canReverse() {
        return can(PaymentAction.REVERSE);
    }

    public PaymentVisibilityScope requireSearchVisibility() {
        AuthenticatedUser user = require(
                PaymentAction.SEARCH
        );

        return partnerIsolation.visibilityFor(user);
    }

    public void requireObjectAccess(
            PaymentAction action,
            PaymentObjectAccessDescriptor object
    ) {
        AuthenticatedUser user = require(action);

        if (!partnerIsolation.canAccess(user, object)) {
            throw new PaymentAccessDeniedException(
                    "Authenticated principal cannot access Payment "
                            + object.paymentId()
            );
        }
    }

    private boolean can(PaymentAction action) {
        return currentUserProvider.currentUser()
                .map(user ->
                        rolePolicy.permits(user, action)
                                && hasRequiredAuthority(
                                user,
                                action
                        )
                )
                .orElse(false);
    }

    private AuthenticatedUser require(
            PaymentAction action
    ) {
        AuthenticatedUser user =
                currentUserProvider.requireCurrentUser();

        if (!rolePolicy.permits(user, action)
                || !hasRequiredAuthority(user, action)) {
            throw new PaymentAccessDeniedException(
                    "Authenticated principal is not allowed to "
                            + action.name().toLowerCase()
                            + " Payments"
            );
        }

        return user;
    }

    private static boolean hasRequiredAuthority(
            AuthenticatedUser user,
            PaymentAction action
    ) {
        PaymentAuthority authority = switch (action) {
            case SEARCH, READ -> PaymentAuthority.READ;
            case OPERATE -> PaymentAuthority.OPERATE;
            case AUDIT -> PaymentAuthority.AUDIT;
            case RECONCILE -> PaymentAuthority.RECONCILE;
            case REVERSE -> PaymentAuthority.REVERSE;
        };

        return user.hasAuthority(authority.authority());
    }
}
