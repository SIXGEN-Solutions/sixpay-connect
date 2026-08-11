package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.exception.LocalAuthenticationFailedException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalAuthenticationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-11T01:30:00Z");

    @Test
    void authenticatesAndReturnsCanonicalSixpayIdentity() {
        Fixture fixture = new Fixture(activeUser(), true);

        AuthenticatedUser authenticated =
                fixture.service.authenticate(
                        new LocalLoginCommand(
                                " Rodrigue ",
                                "correct-password"
                        )
                );

        assertThat(fixture.loadedUsername).isEqualTo("rodrigue");
        assertThat(authenticated.subject()).isEqualTo("local-subject");
        assertThat(authenticated.username()).isEqualTo("Rodrigue");
        assertThat(authenticated.roles()).containsExactly("ADMIN");
        assertThat(authenticated.permissions())
                .containsExactly("SCOPE_payment.read");
        assertThat(fixture.saved.failedAttempts()).isZero();
        assertThat(fixture.saved.lastAuthenticatedAt()).isEqualTo(NOW);
        assertThat(fixture.audit).singleElement()
                .extracting(LocalAuthenticationAuditEvent::outcome)
                .isEqualTo(LocalAuthenticationAuditOutcome.SUCCESS);
    }

    @Test
    void wrongPasswordPersistsFailureAndAuditsGenericFailure() {
        Fixture fixture = new Fixture(activeUser(), false);

        assertThatThrownBy(() ->
                fixture.service.authenticate(
                        new LocalLoginCommand("rodrigue", "wrong")
                )
        ).isInstanceOf(LocalAuthenticationFailedException.class);

        assertThat(fixture.saved.failedAttempts()).isEqualTo(1);
        assertThat(fixture.audit).singleElement()
                .extracting(LocalAuthenticationAuditEvent::outcome)
                .isEqualTo(LocalAuthenticationAuditOutcome.FAILURE);
    }

    @Test
    void unknownUsernameExecutesDummyPasswordVerification() {
        Fixture fixture = new Fixture(null, false);

        assertThatThrownBy(() ->
                fixture.service.authenticate(
                        new LocalLoginCommand("missing", "guess")
                )
        ).isInstanceOf(LocalAuthenticationFailedException.class);

        assertThat(fixture.dummyVerificationCount).isEqualTo(1);
    }

    @Test
    void disabledAccountFailsClosedAndUsesDummyVerification() {
        LocalAuthenticationUser disabled = new LocalAuthenticationUser(
                UUID.randomUUID(),
                "disabled-subject",
                "Disabled",
                "$2a$12$hash",
                LocalAuthenticationAccountStatus.DISABLED,
                Set.of("ROLE_ADMIN"),
                0,
                null,
                null
        );

        Fixture fixture = new Fixture(disabled, true);

        assertThatThrownBy(() ->
                fixture.service.authenticate(
                        new LocalLoginCommand("disabled", "password")
                )
        ).isInstanceOf(LocalAuthenticationFailedException.class);

        assertThat(fixture.dummyVerificationCount).isEqualTo(1);
    }

    private static LocalAuthenticationUser activeUser() {
        return new LocalAuthenticationUser(
                UUID.randomUUID(),
                "local-subject",
                "Rodrigue",
                "$2a$12$hash",
                LocalAuthenticationAccountStatus.ACTIVE,
                Set.of("ROLE_ADMIN", "SCOPE_payment.read"),
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
            this.loaded = loaded;
            this.passwordMatches = passwordMatches;
            TimeProvider timeProvider = () -> NOW;

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
            this.loadedUsername = normalizedUsername;
            return Optional.ofNullable(loaded);
        }

        @Override
        public void saveAuthenticationState(
                LocalAuthenticationUser user
        ) {
            this.saved = user;
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
