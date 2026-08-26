package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationUserTest {

    private static final Instant NOW = Instant.parse("2026-08-11T01:30:00Z");
    private static final PasswordPolicy POLICY = new PasswordPolicy(12, 200, 5, 90);

    @Test
    void activeRequiresBothLocalIdentityAndCanonicalUserToBeActive() {
        LocalAuthenticationUser active = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.ACTIVE);
        LocalAuthenticationUser disabledAccount = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.DISABLED);
        assertThat(active.active()).isTrue();
        assertThat(disabledAccount.active()).isFalse();
    }

    @Test
    void canonicalSubjectIsSixpayUserId() {
        LocalAuthenticationUser user = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.ACTIVE);
        assertThat(user.canonicalSubject()).isEqualTo(user.userId().toString());
    }

    @Test
    void locksAtConfiguredThreshold() {
        LocalAuthenticationUser user = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.ACTIVE);
        LocalAuthenticationUser failed = user.authenticationFailed(NOW, 1, Duration.ofMinutes(15));
        assertThat(failed.lockedAt(NOW)).isTrue();
    }

    @Test
    void userPasswordChangeActivatesLifecycleWithoutChangingIdentity() {
        LocalAuthenticationUser user = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.ACTIVE);
        LocalAuthenticationUser changed = user.withUserChangedPassword("$2a$12$changed", NOW, POLICY);
        assertThat(changed.userId()).isEqualTo(user.userId());
        assertThat(changed.mustChangePassword()).isFalse();
        assertThat(changed.passwordChangedAt()).isEqualTo(NOW);
        assertThat(changed.expiresAt()).isEqualTo(NOW.plusSeconds(90L * 24L * 60L * 60L));
    }

    @Test
    void administrativeResetRequiresAnotherUserPasswordChange() {
        LocalAuthenticationUser changed = user(LocalAuthenticationAccountStatus.ACTIVE, SixpayUserAccountStatus.ACTIVE)
                .withUserChangedPassword("$2a$12$changed", NOW, POLICY);
        LocalAuthenticationUser reset = changed.withAdministrativelyResetPassword("$2a$12$temporary", NOW.plusSeconds(60));
        assertThat(reset.mustChangePassword()).isTrue();
        assertThat(reset.passwordChangedAt()).isNull();
        assertThat(reset.expiresAt()).isNull();
        assertThat(reset.failedAttempts()).isZero();
        assertThat(reset.lockedUntil()).isNull();
    }

    private static LocalAuthenticationUser user(LocalAuthenticationAccountStatus localStatus, SixpayUserAccountStatus accountStatus) {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        return new LocalAuthenticationUser(
                UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                userId,
                "local-provider-subject",
                "rodrigue",
                LocalCredential.provisioned(userId, "$2a$12$hash", NOW.minusSeconds(60)),
                localStatus,
                accountStatus,
                Set.of("ROLE_ADMIN"),
                0,
                null,
                null
        );
    }
}
