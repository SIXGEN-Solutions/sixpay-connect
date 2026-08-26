package com.sixpay.security.application.service;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HybridIdentityConvergenceTest {

    @Test
    void localAndOidcRepresentSameCanonicalUser() {
        UUID userId =
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

        SixpayUserAccount account =
                new SixpayUserAccount(
                        userId,
                        "rodrigue",
                        "rodrigue@sixpay.test",
                        SixpayUserAccountStatus.ACTIVE,
                        Set.of("ADMIN"),
                        Set.of("SCOPE_payment.read")
                );

        AuthenticatedUser localUser =
                new AuthenticatedUser(
                        account.canonicalSubject(),
                        account.username(),
                        account.authorities()
                );

        Instant now = Instant.parse("2026-08-11T02:00:00Z");
        UserIdentity oidcIdentity =
                new UserIdentity(
                        UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                        userId,
                        AuthenticationIdentityType.OIDC,
                        "https://idp.example.test",
                        "oidc-provider-subject",
                        now,
                        now
                );

        LinkedExternalIdentityResolver resolver =
                new LinkedExternalIdentityResolver(
                        (type, provider, subject) ->
                                Optional.of(
                                        new LinkedUserIdentity(
                                                account,
                                                oidcIdentity
                                        )
                                )
                );

        AuthenticatedUser oidcUser =
                resolver.resolve(
                        new ExternalIdentity(
                                "https://idp.example.test",
                                "oidc-provider-subject",
                                "provider-email@sixpay.test"
                        )
                );

        assertThat(localUser.subject())
                .isEqualTo(oidcUser.subject());
        assertThat(localUser.username())
                .isEqualTo(oidcUser.username());
        assertThat(localUser.authorities())
                .isEqualTo(oidcUser.authorities());
    }
}
