package com.sixpay.partner.api.security;

import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("partnerAccessPolicy")
public class PartnerAccessPolicy {

    private static final Set<SixpayRole> INTERNAL_ROLES = Set.of(
            SixpayRole.ADMIN,
            SixpayRole.MANAGER,
            SixpayRole.AUDITOR
    );

    private final CurrentUserProvider currentUserProvider;

    public PartnerAccessPolicy(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * The security module must map a partner credential (mTLS or API key) to a
     * PARTNER authority and use the partner UUID as the authenticated subject.
     */
    public boolean canRead(UUID partnerId) {
        return currentUserProvider.currentUser()
                .map(user -> INTERNAL_ROLES.stream().anyMatch(user::hasRole)
                        || (user.hasRole(SixpayRole.PARTNER)
                        && partnerId.toString().equals(user.subject())))
                .orElse(false);
    }
}
