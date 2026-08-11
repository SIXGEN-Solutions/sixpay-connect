package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationUserTest {

    private static final Instant NOW =
            Instant.parse("2026-08-11T01:30:00Z");

    @Test
    void locksAtConfiguredThreshold() {
        LocalAuthenticationUser user = activeUser(0, null);

        LocalAuthenticationUser first =
                user.authenticationFailed(
                        NOW,
                        2,
                        Duration.ofMinutes(15)
                );

        LocalAuthenticationUser second =
                first.authenticationFailed(
                        NOW,
                        2,
                        Duration.ofMinutes(15)
                );

        assertThat(first.lockedUntil()).isNull();
        assertThat(second.failedAttempts()).isEqualTo(2);
        assertThat(second.lockedAt(NOW)).isTrue();
    }

    @Test
    void unlocksExpiredTemporaryLock() {
        LocalAuthenticationUser user =
                activeUser(5, NOW.minusSeconds(1));

        LocalAuthenticationUser unlocked =
                user.unlockIfExpired(NOW);

        assertThat(unlocked.failedAttempts()).isZero();
        assertThat(unlocked.lockedUntil()).isNull();
    }

    @Test
    void successResetsFailuresAndRecordsAuthenticationTime() {
        LocalAuthenticationUser user =
                activeUser(3, null);

        LocalAuthenticationUser authenticated =
                user.authenticationSucceeded(NOW);

        assertThat(authenticated.failedAttempts()).isZero();
        assertThat(authenticated.lockedUntil()).isNull();
        assertThat(authenticated.lastAuthenticatedAt())
                .isEqualTo(NOW);
    }

    private static LocalAuthenticationUser activeUser(
            int failedAttempts,
            Instant lockedUntil
    ) {
        return new LocalAuthenticationUser(
                UUID.randomUUID(),
                "local-subject",
                "Rodrigue",
                "$2a$12$hash",
                LocalAuthenticationAccountStatus.ACTIVE,
                Set.of("ROLE_ADMIN", "SCOPE_payment.read"),
                failedAttempts,
                lockedUntil,
                null
        );
    }
}
