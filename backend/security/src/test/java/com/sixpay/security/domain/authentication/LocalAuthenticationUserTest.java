package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationUserTest {

    private static final Instant NOW = Instant.parse("2026-08-11T01:30:00Z");

    @Test
    void activeRequiresBothLocalIdentityAndCanonicalUserToBeActive() {
        LocalAuthenticationUser active = user(
                LocalAuthenticationAccountStatus.ACTIVE,
                SixpayUserAccountStatus.ACTIVE
        );
        LocalAuthenticationUser disabledAccount = user(
                LocalAuthenticationAccountStatus.ACTIVE,
                SixpayUserAccountStatus.DISABLED
        );

        assertThat(active.active()).isTrue();
        assertThat(disabledAccount.active()).isFalse();
    }

    @Test
    void canonicalSubjectIsSixpayUserId() {
        LocalAuthenticationUser user = user(
                LocalAuthenticationAccountStatus.ACTIVE,
                SixpayUserAccountStatus.ACTIVE
        );

        assertThat(user.canonicalSubject())
                .isEqualTo(user.userId().toString());
    }

    @Test
    void locksAtConfiguredThreshold() {
        LocalAuthenticationUser user = user(
                LocalAuthenticationAccountStatus.ACTIVE,
                SixpayUserAccountStatus.ACTIVE
        );

        LocalAuthenticationUser failed =
                user.authenticationFailed(NOW, 1, Duration.ofMinutes(15));

        assertThat(failed.lockedAt(NOW)).isTrue();
    }

    private static LocalAuthenticationUser user(
            LocalAuthenticationAccountStatus localStatus,
            SixpayUserAccountStatus accountStatus
    ) {
        return new LocalAuthenticationUser(
                UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"),
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                "local-provider-subject",
                "rodrigue",
                "$2a$12$hash",
                localStatus,
                accountStatus,
                Set.of("ROLE_ADMIN"),
                0,
                null,
                null
        );
    }
}
