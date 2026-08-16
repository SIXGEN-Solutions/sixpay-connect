package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthenticationUserJpaEntityTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-08-15T20:00:00Z");
    private static final PasswordPolicy POLICY = new PasswordPolicy(12, 200, 5, 90);

    @Test
    void provisionedCredentialStartsInMustChangeState() {
        LocalAuthenticationUserJpaEntity entity = provisioned();
        assertThat(entity.isMustChangePassword()).isTrue();
        assertThat(entity.getPasswordChangedAt()).isNull();
        assertThat(entity.getExpiresAt()).isNull();
        assertThat(entity.getCredentialUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void administrativeResetRestoresTemporaryLifecycle() {
        LocalAuthenticationUserJpaEntity entity = provisioned();
        entity.changePassword("$2a$12$changed", NOW.plusSeconds(60), POLICY);
        entity.resetPassword("$2a$12$temporary", NOW.plusSeconds(120));
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$12$temporary");
        assertThat(entity.isMustChangePassword()).isTrue();
        assertThat(entity.getPasswordChangedAt()).isNull();
        assertThat(entity.getExpiresAt()).isNull();
    }

    @Test
    void userChangeComputesExpirationFromPolicy() {
        LocalAuthenticationUserJpaEntity entity = provisioned();
        Instant changedAt = NOW.plusSeconds(60);
        entity.changePassword("$2a$12$changed", changedAt, POLICY);
        assertThat(entity.isMustChangePassword()).isFalse();
        assertThat(entity.getPasswordChangedAt()).isEqualTo(changedAt);
        assertThat(entity.getExpiresAt()).isEqualTo(changedAt.plusSeconds(90L * 24L * 60L * 60L));
        assertThat(entity.getCredentialUpdatedAt()).isEqualTo(changedAt);
    }

    private static LocalAuthenticationUserJpaEntity provisioned() {
        SecurityUserAccountJpaEntity account = SecurityUserAccountJpaEntity.create(
                USER_ID,
                "admin",
                "admin@sixpay.local",
                Set.of("ADMIN"),
                Set.of("payment.read"),
                NOW
        );
        return LocalAuthenticationUserJpaEntity.provisioned(account, "$2a$12$temporary", NOW);
    }
}
