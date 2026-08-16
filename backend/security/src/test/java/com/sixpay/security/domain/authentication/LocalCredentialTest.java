package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalCredentialTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final Instant NOW = Instant.parse("2026-08-15T20:00:00Z");
    private static final PasswordPolicy POLICY = new PasswordPolicy(12, 200, 5, 90);

    @Test
    void administrativeProvisioningCreatesTemporaryCredential() {
        LocalCredential credential = LocalCredential.provisioned(USER_ID, "$2a$12$initial", NOW);
        assertThat(credential.mustChangePassword()).isTrue();
        assertThat(credential.passwordChangedAt()).isNull();
        assertThat(credential.expiresAt()).isNull();
        assertThat(credential.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void administrativeResetReturnsCredentialToMustChangeState() {
        LocalCredential active = LocalCredential.provisioned(USER_ID, "$2a$12$initial", NOW)
                .changedByUser("$2a$12$user-change", NOW.plusSeconds(60), POLICY);

        LocalCredential reset = active.reset("$2a$12$temporary", NOW.plusSeconds(120));
        assertThat(reset.passwordHash()).isEqualTo("$2a$12$temporary");
        assertThat(reset.mustChangePassword()).isTrue();
        assertThat(reset.passwordChangedAt()).isNull();
        assertThat(reset.expiresAt()).isNull();
    }

    @Test
    void userPasswordChangeStartsConfiguredLifetime() {
        Instant changedAt = NOW.plusSeconds(60);
        LocalCredential changed = LocalCredential.provisioned(USER_ID, "$2a$12$temporary", NOW)
                .changedByUser("$2a$12$changed", changedAt, POLICY);

        assertThat(changed.mustChangePassword()).isFalse();
        assertThat(changed.passwordChangedAt()).isEqualTo(changedAt);
        assertThat(changed.expiresAt()).isEqualTo(changedAt.plusSeconds(90L * 24L * 60L * 60L));
        assertThat(changed.updatedAt()).isEqualTo(changedAt);
    }

    @Test
    void expirationIsEffectiveAtBoundary() {
        LocalCredential changed = LocalCredential.provisioned(USER_ID, "$2a$12$temporary", NOW)
                .changedByUser("$2a$12$changed", NOW, POLICY);
        assertThat(changed.expiredAt(changed.expiresAt().minusNanos(1))).isFalse();
        assertThat(changed.expiredAt(changed.expiresAt())).isTrue();
        assertThat(changed.changeRequiredAt(changed.expiresAt())).isTrue();
    }

    @Test
    void rejectsExpirationWithoutPasswordChangeTimestamp() {
        assertThatThrownBy(() -> new LocalCredential(
                USER_ID, "$2a$12$hash", false, null, NOW.plusSeconds(3600), NOW
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("expiration requires");
    }
}
