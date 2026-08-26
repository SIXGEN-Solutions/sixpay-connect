package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.port.in.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.out.*;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationPasswordLifecycleIT {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-08-16T03:00:00Z"
            );

    private static final PasswordPolicy POLICY =
            new PasswordPolicy(
                    12,
                    200,
                    5,
                    90
            );

    @Test
    void temporaryPasswordRestrictsSessionUntilUserChangesCredential() {
        PasswordEncoder encoder =
                new BCryptPasswordEncoder(4);

        MutableSecurityStore store =
                new MutableSecurityStore(
                        encoder.encode(
                                "Temporary-password-2026"
                        )
                );

        TimeProvider timeProvider =
                () -> NOW;

        LocalAuthenticationService authenticationService =
                new LocalAuthenticationService(
                        store,
                        store,
                        store,
                        store,
                        timeProvider,
                        5,
                        Duration.ofMinutes(15)
                );

        LocalPasswordChangeService passwordChangeService =
                new LocalPasswordChangeService(
                        store,
                        store,
                        encoder,
                        POLICY,
                        store,
                        timeProvider
                );

        AuthenticatedUser firstLogin =
                authenticationService.authenticate(
                        new LocalLoginCommand(
                                "manager",
                                "Temporary-password-2026"
                        )
                );

        assertThat(
                firstLogin.passwordChangeRequired()
        )
                .isTrue();

        passwordChangeService.changePassword(
                new ChangeLocalPasswordCommand(
                        USER_ID,
                        USER_ID.toString(),
                        "Temporary-password-2026",
                        "Permanent-password-2027"
                )
        );

        AuthenticatedUser secondLogin =
                authenticationService.authenticate(
                        new LocalLoginCommand(
                                "manager",
                                "Permanent-password-2027"
                        )
                );

        assertThat(
                secondLogin.passwordChangeRequired()
        )
                .isFalse();

        assertThat(
                store.user.mustChangePassword()
        )
                .isFalse();

        assertThat(
                store.user.passwordChangedAt()
        )
                .isEqualTo(NOW);

        assertThat(
                store.user.expiresAt()
        )
                .isEqualTo(
                        NOW.plus(
                                Duration.ofDays(90)
                        )
                );

        assertThat(
                store.passwordHistory
        )
                .hasSize(1);

        assertThat(
                store.securityAudit
                        .stream()
                        .map(SecurityAuditEvent::eventType)
        )
                .contains(
                        com.sixpay.security.domain.administration.SecurityAuditEventType.PASSWORD_CHANGED
                );
    }

    private static final class MutableSecurityStore
            implements LoadAuthenticationUserPort,
            SaveAuthenticationUserStatePort,
            PasswordVerificationPort,
            AuthenticationAuditPort,
            PasswordHistoryPort,
            ChangeLocalCredentialPort,
            SecurityAuditPort {

        private final PasswordEncoder encoder =
                new BCryptPasswordEncoder(4);

        private LocalAuthenticationUser user;

        private final List<String> passwordHistory =
                new ArrayList<>();

        private final List<SecurityAuditEvent> securityAudit =
                new ArrayList<>();

        private MutableSecurityStore(
                String temporaryPasswordHash
        ) {
            LocalCredential credential =
                    LocalCredential.provisioned(
                            USER_ID,
                            temporaryPasswordHash,
                            NOW.minusSeconds(60)
                    );

            this.user =
                    new LocalAuthenticationUser(
                            UUID.fromString(
                                    "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
                            ),
                            USER_ID,
                            "manager-local",
                            "manager",
                            credential,
                            LocalAuthenticationAccountStatus.ACTIVE,
                            SixpayUserAccountStatus.ACTIVE,
                            Set.of(
                                    "ROLE_MANAGER",
                                    "SCOPE_payment.read"
                            ),
                            0,
                            null,
                            null
                    );
        }

        @Override
        public Optional<LocalAuthenticationUser>
        loadForAuthentication(
                String normalizedUsername
        ) {
            return "manager".equals(
                    normalizedUsername
            )
                    ? Optional.of(user)
                    : Optional.empty();
        }

        @Override
        public void saveAuthenticationState(
                LocalAuthenticationUser user
        ) {
            this.user = user;
        }

        @Override
        public boolean matches(
                CharSequence rawPassword,
                String passwordHash
        ) {
            return encoder.matches(
                    rawPassword,
                    passwordHash
            );
        }

        @Override
        public void performDummyVerification(
                CharSequence rawPassword
        ) {
            encoder.matches(
                    rawPassword,
                    encoder.encode(
                            "dummy-password"
                    )
            );
        }

        @Override
        public void record(
                LocalAuthenticationAuditEvent event
        ) {
            // Authentication audit is not the assertion target of this IT.
        }

        @Override
        public PasswordHistorySnapshot
        loadForPasswordReplacement(
                UUID userId,
                int historySize
        ) {
            return new PasswordHistorySnapshot(
                    user.passwordHash(),
                    passwordHistory
                            .stream()
                            .limit(historySize)
                            .toList()
            );
        }

        @Override
        public void archiveReplacedPassword(
                UUID userId,
                String replacedPasswordHash,
                Instant createdAt,
                int historySize
        ) {
            passwordHistory.add(
                    0,
                    replacedPasswordHash
            );

            while (
                    passwordHistory.size()
                            > historySize
            ) {
                passwordHistory.remove(
                        passwordHistory.size() - 1
                );
            }
        }

        @Override
        public void changePassword(
                UUID userId,
                String newPasswordHash,
                Instant changedAt,
                PasswordPolicy policy
        ) {
            this.user =
                    this.user
                            .withUserChangedPassword(
                                    newPasswordHash,
                                    changedAt,
                                    policy
                            );
        }

        @Override
        public void record(
                SecurityAuditEvent event
        ) {
            securityAudit.add(event);
        }
    }
}
