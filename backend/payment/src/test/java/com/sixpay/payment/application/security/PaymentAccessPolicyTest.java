package com.sixpay.payment.application.security;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authorization.SixpayRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAccessPolicyTest {

    @Test
    void partnerReceivesSubjectBoundVisibility() {
        AuthenticatedUser partner = user(
                "partner-001",
                SixpayRole.PARTNER,
                PaymentAuthority.READ
        );

        PaymentAccessPolicy policy = policy(partner);

        assertThat(policy.requireSearchVisibility())
                .isEqualTo(
                        new PaymentVisibilityScope.Partner(
                                "partner-001"
                        )
                );
    }

    @Test
    void partnerCanReadOnlyOwnedPayment() {
        AuthenticatedUser partner = user(
                "partner-001",
                SixpayRole.PARTNER,
                PaymentAuthority.READ
        );

        PaymentAccessPolicy policy = policy(partner);

        var owned = new PaymentObjectAccessDescriptor(
                new PaymentId(UUID.randomUUID()),
                PaymentSource.TRESOR_PAY,
                "partner-001"
        );

        policy.requireObjectAccess(
                PaymentAction.READ,
                owned
        );

        var foreign = new PaymentObjectAccessDescriptor(
                new PaymentId(UUID.randomUUID()),
                PaymentSource.TRESOR_PAY,
                "partner-002"
        );

        assertThatThrownBy(() ->
                policy.requireObjectAccess(
                        PaymentAction.READ,
                        foreign
                )
        ).isInstanceOf(
                PaymentAccessDeniedException.class
        );
    }

    @Test
    void partnerAccessFailsClosedWithoutOwner() {
        AuthenticatedUser partner = user(
                "partner-001",
                SixpayRole.PARTNER,
                PaymentAuthority.READ
        );

        PaymentAccessPolicy policy = policy(partner);

        var unknownOwner =
                new PaymentObjectAccessDescriptor(
                        new PaymentId(UUID.randomUUID()),
                        PaymentSource.TRESOR_PAY,
                        null
                );

        assertThatThrownBy(() ->
                policy.requireObjectAccess(
                        PaymentAction.READ,
                        unknownOwner
                )
        ).isInstanceOf(
                PaymentAccessDeniedException.class
        );
    }

    @Test
    void readOnlyRoleCannotOperate() {
        AuthenticatedUser readOnly = user(
                "reader-001",
                SixpayRole.READ_ONLY,
                PaymentAuthority.READ
        );

        PaymentAccessPolicy policy = policy(readOnly);

        assertThat(policy.canRead()).isTrue();
        assertThat(policy.canOperate()).isFalse();
    }

    private static PaymentAccessPolicy policy(
            AuthenticatedUser user
    ) {
        CurrentUserProvider provider =
                () -> Optional.of(user);

        return new PaymentAccessPolicy(
                provider,
                new PaymentRolePolicy(),
                new PaymentPartnerIsolationPolicy()
        );
    }

    private static AuthenticatedUser user(
            String subject,
            SixpayRole role,
            PaymentAuthority authority
    ) {
        return new AuthenticatedUser(
                subject,
                subject,
                Set.of(
                        role.authority(),
                        authority.authority()
                )
        );
    }
}
