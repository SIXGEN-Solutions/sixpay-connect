package com.sixpay.security.application.service;

import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HybridAuthorizationConvergenceTest {

    @Test
    void sameCanonicalUserHasSameAuthorizationForLocalAndOidc() {
        UUID userId =
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

        SixpayUserAccount account =
                new SixpayUserAccount(
                        userId,
                        "rodrigue",
                        "rodrigue@sixpay.test",
                        SixpayUserAccountStatus.ACTIVE,
                        Set.of("ADMIN", "AUDITOR"),
                        Set.of(
                                "SCOPE_payment.read",
                                "payment.export"
                        )
                );

        AuthenticatedUser localPrincipal =
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
                        "external-subject",
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

        AuthenticatedUser oidcPrincipal =
                resolver.resolve(
                        new ExternalIdentity(
                                "https://idp.example.test",
                                "external-subject",
                                "provider-user@example.test"
                        )
                );

        assertThat(oidcPrincipal.subject())
                .isEqualTo(localPrincipal.subject());
        assertThat(oidcPrincipal.roles())
                .isEqualTo(localPrincipal.roles());
        assertThat(oidcPrincipal.permissions())
                .isEqualTo(localPrincipal.permissions());
        assertThat(oidcPrincipal.authorities())
                .isEqualTo(localPrincipal.authorities());
    }
}
