package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditOutcome;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LocalLogoutServiceTest {

    @Test
    void auditsSuccessfulLogout() {
        Instant now = Instant.parse("2026-08-11T01:30:00Z");
        AtomicReference<LocalAuthenticationAuditEvent> recorded =
                new AtomicReference<>();
        TimeProvider timeProvider = () -> now;

        LocalLogoutService service = new LocalLogoutService(
                recorded::set,
                timeProvider
        );

        service.logout(
                new AuthenticatedUser(
                        "local-subject",
                        "rodrigue",
                        Set.of("ROLE_ADMIN")
                )
        );

        assertThat(recorded.get().type())
                .isEqualTo(LocalAuthenticationAuditType.LOGOUT);
        assertThat(recorded.get().outcome())
                .isEqualTo(LocalAuthenticationAuditOutcome.SUCCESS);
        assertThat(recorded.get().occurredAt()).isEqualTo(now);
    }
}
