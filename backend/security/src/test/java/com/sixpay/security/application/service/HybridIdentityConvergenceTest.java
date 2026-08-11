package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.out.PasswordVerificationPort;
import com.sixpay.security.application.port.out.SaveAuthenticationUserStatePort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HybridIdentityConvergenceTest {

    @Test
    void localAndOidcIdentitiesResolveToSameCanonicalSixpaySubject() {
        UUID userId =
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        Instant now = Instant.parse("2026-08-11T02:00:00Z");

        LocalAuthenticationUser localIdentity =
                new LocalAuthenticationUser(
                        UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                        userId,
                        "local-provider-subject",
                        "rodrigue",
                        "$2a$12$hash",
                        LocalAuthenticationAccountStatus.ACTIVE,
                        SixpayUserAccountStatus.ACTIVE,
                        Set.of("ROLE_ADMIN"),
                        0,
                        null,
                        null
                );

        LocalFixture localFixture =
                new LocalFixture(localIdentity, now);

        AuthenticatedUser localUser =
                localFixture.service.authenticate(
                        new LocalLoginCommand(
                                "rodrigue",
                                "correct-password"
                        )
                );

        SixpayUserAccount account =
                new SixpayUserAccount(
                        userId,
                        "rodrigue",
                        "rodrigue@sixpay.test",
                        SixpayUserAccountStatus.ACTIVE
                );

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

        LinkedExternalIdentityResolver oidcResolver =
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
                oidcResolver.resolve(
                        new ExternalIdentity(
                                "https://idp.example.test",
                                "oidc-provider-subject",
                                "provider-email@sixpay.test"
                        ),
                        Set.of("ROLE_ADMIN")
                );

        assertThat(localUser.subject())
                .isEqualTo(userId.toString());
        assertThat(oidcUser.subject())
                .isEqualTo(userId.toString());
        assertThat(localUser.subject())
                .isEqualTo(oidcUser.subject());
        assertThat(localUser.username())
                .isEqualTo(oidcUser.username());
    }

    private static final class LocalFixture
            implements LoadAuthenticationUserPort,
            SaveAuthenticationUserStatePort,
            PasswordVerificationPort,
            AuthenticationAuditPort {

        private final LocalAuthenticationUser user;
        private final LocalAuthenticationService service;

        private LocalFixture(
                LocalAuthenticationUser user,
                Instant now
        ) {
            this.user = user;
            TimeProvider timeProvider = () -> now;
            this.service = new LocalAuthenticationService(
                    this,
                    this,
                    this,
                    this,
                    timeProvider,
                    5,
                    Duration.ofMinutes(15)
            );
        }

        @Override
        public Optional<LocalAuthenticationUser> loadForAuthentication(
                String normalizedUsername
        ) {
            return Optional.of(user);
        }

        @Override
        public void saveAuthenticationState(LocalAuthenticationUser user) {
        }

        @Override
        public boolean matches(
                CharSequence rawPassword,
                String passwordHash
        ) {
            return true;
        }

        @Override
        public void performDummyVerification(CharSequence rawPassword) {
        }

        @Override
        public void record(LocalAuthenticationAuditEvent event) {
        }
    }
}
