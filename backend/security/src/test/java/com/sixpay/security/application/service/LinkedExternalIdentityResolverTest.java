package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinkedExternalIdentityResolverTest {

    private static final UUID USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @Test
    void resolvesAuthorizationFromSixpayUserNotProviderClaims() {
        LinkedExternalIdentityResolver resolver =
                new LinkedExternalIdentityResolver(
                        (type, provider, subject) ->
                                Optional.of(
                                        linkedUser(
                                                SixpayUserAccountStatus.ACTIVE
                                        )
                                )
                );

        AuthenticatedUser user = resolver.resolve(
                new ExternalIdentity(
                        "https://idp.example.test",
                        "provider-subject-123",
                        "provider-user@example.test"
                )
        );

        assertThat(user.subject()).isEqualTo(USER_ID.toString());
        assertThat(user.username()).isEqualTo("rodrigue");
        assertThat(user.roles()).containsExactly("ADMIN");
        assertThat(user.permissions())
                .containsExactly("SCOPE_payment.read");
    }

    @Test
    void refusesUnknownExternalIdentity() {
        LinkedExternalIdentityResolver resolver =
                new LinkedExternalIdentityResolver(
                        (type, provider, subject) -> Optional.empty()
                );

        assertThatThrownBy(() ->
                resolver.resolve(
                        new ExternalIdentity(
                                "https://idp.example.test",
                                "unknown-subject",
                                "someone@example.test"
                        )
                )
        ).isInstanceOf(ExternalIdentityNotLinkedException.class);
    }

    @Test
    void refusesDisabledCanonicalUser() {
        LinkedExternalIdentityResolver resolver =
                new LinkedExternalIdentityResolver(
                        (type, provider, subject) ->
                                Optional.of(
                                        linkedUser(
                                                SixpayUserAccountStatus.DISABLED
                                        )
                                )
                );

        assertThatThrownBy(() ->
                resolver.resolve(
                        new ExternalIdentity(
                                "https://idp.example.test",
                                "provider-subject-123",
                                "changed-email@example.test"
                        )
                )
        ).isInstanceOf(SixpayUserDisabledException.class);
    }

    private static LinkedUserIdentity linkedUser(
            SixpayUserAccountStatus status
    ) {
        Instant now = Instant.parse("2026-08-11T02:00:00Z");

        SixpayUserAccount account =
                new SixpayUserAccount(
                        USER_ID,
                        "rodrigue",
                        "rodrigue@sixpay.test",
                        status,
                        Set.of("ADMIN"),
                        Set.of("SCOPE_payment.read")
                );

        UserIdentity identity =
                new UserIdentity(
                        UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                        USER_ID,
                        AuthenticationIdentityType.OIDC,
                        "https://idp.example.test",
                        "provider-subject-123",
                        now,
                        now
                );

        return new LinkedUserIdentity(account, identity);
    }
}
