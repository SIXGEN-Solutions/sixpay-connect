package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.exception.LocalAuthenticationFailedException;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.out.PasswordVerificationPort;
import com.sixpay.security.application.port.out.SaveAuthenticationUserStatePort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.LocalAuthenticationAccountStatus;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;
import com.sixpay.security.domain.authentication.LocalAuthenticationUser;
import com.sixpay.security.domain.authentication.LocalCredential;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalAuthenticationServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-08-11T01:30:00Z"
            );

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final PasswordPolicy POLICY =
            new PasswordPolicy(
                    12,
                    200,
                    5,
                    90
            );

    @Test
    void validTemporaryPasswordCreatesRestrictedAuthenticatedPrincipal() {
        Fixture fixture =
                new Fixture(
                        temporaryUser(
                                SixpayUserAccountStatus.ACTIVE
                        ),
                        true
                );

        AuthenticatedUser authenticated =
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                " Rodrigue ",
                                "correct-password"
                        )
                );

        assertThat(
                fixture.loadedUsername
        ).isEqualTo(
                "rodrigue"
        );

        assertThat(
                authenticated.subject()
        ).isEqualTo(
                USER_ID.toString()
        );

        assertThat(
                authenticated.username()
        ).isEqualTo(
                "Rodrigue"
        );

        assertThat(
                authenticated.roles()
        ).containsExactly(
                "ADMIN"
        );

        assertThat(
                authenticated.passwordChangeRequired()
        ).isTrue();

        assertThat(
                fixture.saved.failedAttempts()
        ).isZero();
    }

    @Test
    void validExpiredPasswordCreatesRestrictedAuthenticatedPrincipal() {
        Fixture fixture =
                new Fixture(
                        expiredUser(),
                        true
                );

        AuthenticatedUser authenticated =
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                "rodrigue",
                                "correct-password"
                        )
                );

        assertThat(
                authenticated.passwordChangeRequired()
        ).isTrue();
    }

    @Test
    void validNormalPasswordCreatesNormalAuthenticatedPrincipal() {
        Fixture fixture =
                new Fixture(
                        normalUser(),
                        true
                );

        AuthenticatedUser authenticated =
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                "rodrigue",
                                "correct-password"
                        )
                );

        assertThat(
                authenticated.passwordChangeRequired()
        ).isFalse();
    }

    @Test
    void rejectsLocalCredentialsWhenCanonicalUserIsDisabled() {
        Fixture fixture =
                new Fixture(
                        temporaryUser(
                                SixpayUserAccountStatus.DISABLED
                        ),
                        true
                );

        assertThatThrownBy(() ->
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                "rodrigue",
                                "correct-password"
                        )
                )
        )
                .isInstanceOf(
                        LocalAuthenticationFailedException.class
                );

        assertThat(
                fixture.dummyVerificationCount
        ).isEqualTo(1);
    }

    @Test
    void wrongPasswordPersistsFailureWithoutChangingCredentialLifecycle() {
        Fixture fixture =
                new Fixture(
                        temporaryUser(
                                SixpayUserAccountStatus.ACTIVE
                        ),
                        false
                );

        assertThatThrownBy(() ->
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                "rodrigue",
                                "wrong"
                        )
                )
        )
                .isInstanceOf(
                        LocalAuthenticationFailedException.class
                );

        assertThat(
                fixture.saved.failedAttempts()
        ).isEqualTo(1);

        assertThat(
                fixture.saved.mustChangePassword()
        ).isTrue();
    }

    private static LocalAuthenticationUser
    temporaryUser(
            SixpayUserAccountStatus accountStatus
    ) {
        return user(
                LocalCredential.provisioned(
                        USER_ID,
                        "$2a$12$hash",
                        NOW.minusSeconds(60)
                ),
                accountStatus
        );
    }

    private static LocalAuthenticationUser
    normalUser() {
        LocalCredential credential =
                LocalCredential.provisioned(
                                USER_ID,
                                "$2a$12$hash",
                                NOW.minusSeconds(120)
                        )
                        .changedByUser(
                                "$2a$12$normal",
                                NOW.minusSeconds(60),
                                POLICY
                        );

        return user(
                credential,
                SixpayUserAccountStatus.ACTIVE
        );
    }

    private static LocalAuthenticationUser
    expiredUser() {
        Instant changedAt =
                NOW.minusSeconds(
                        91L * 24L * 60L * 60L
                );

        LocalCredential credential =
                LocalCredential.provisioned(
                                USER_ID,
                                "$2a$12$hash",
                                changedAt.minusSeconds(60)
                        )
                        .changedByUser(
                                "$2a$12$expired",
                                changedAt,
                                POLICY
                        );

        return user(
                credential,
                SixpayUserAccountStatus.ACTIVE
        );
    }

    private static LocalAuthenticationUser user(
            LocalCredential credential,
            SixpayUserAccountStatus accountStatus
    ) {
        return new LocalAuthenticationUser(
                UUID.fromString(
                        "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
                ),
                USER_ID,
                "local-provider-subject",
                "Rodrigue",
                credential,
                LocalAuthenticationAccountStatus.ACTIVE,
                accountStatus,
                Set.of(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read"
                ),
                0,
                null,
                null
        );
    }

    private static final class Fixture
            implements LoadAuthenticationUserPort,
            SaveAuthenticationUserStatePort,
            PasswordVerificationPort,
            AuthenticationAuditPort {

        private final LocalAuthenticationUser loaded;
        private final boolean passwordMatches;
        private final List<LocalAuthenticationAuditEvent> audit =
                new ArrayList<>();

        private LocalAuthenticationUser saved;
        private String loadedUsername;
        private int dummyVerificationCount;
        private final LocalAuthenticationService service;

        private Fixture(
                LocalAuthenticationUser loaded,
                boolean passwordMatches
        ) {
            this.loaded =
                    loaded;
            this.passwordMatches =
                    passwordMatches;

            TimeProvider timeProvider =
                    () -> NOW;

            this.service =
                    new LocalAuthenticationService(
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
        public Optional<LocalAuthenticationUser>
        loadForAuthentication(
                String normalizedUsername
        ) {
            loadedUsername =
                    normalizedUsername;

            return Optional.ofNullable(
                    loaded
            );
        }

        @Override
        public void saveAuthenticationState(
                LocalAuthenticationUser user
        ) {
            saved =
                    user;
        }

        @Override
        public boolean matches(
                CharSequence rawPassword,
                String passwordHash
        ) {
            return passwordMatches;
        }

        @Override
        public void performDummyVerification(
                CharSequence rawPassword
        ) {
            dummyVerificationCount++;
        }

        @Override
        public void record(
                LocalAuthenticationAuditEvent event
        ) {
            audit.add(event);
        }
    }
}
