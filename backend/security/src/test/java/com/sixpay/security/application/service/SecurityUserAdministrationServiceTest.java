package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUserAdministrationServiceTest {

    @Test
    void resetPasswordHashesSecretAndAuditsWithoutPassword() {
        FakePort port = new FakePort();
        List<SecurityAuditEvent> audit = new ArrayList<>();
        SecurityAuditPort auditPort = audit::add;

        SecurityUserAdministrationService service =
                new SecurityUserAdministrationService(
                        port,
                        auditPort,
                        new BCryptPasswordEncoder(10)
                );

        service.resetLocalPassword(
                port.userId,
                "VeryStrongPassword-2026",
                "admin-subject"
        );

        assertThat(port.passwordHash).startsWith("$2");
        assertThat(port.passwordHash).doesNotContain("VeryStrongPassword-2026");
        assertThat(audit).singleElement().satisfies(event -> {
            assertThat(event.eventType()).isEqualTo(SecurityAuditEventType.PASSWORD_RESET);
            assertThat(event.detail()).isNull();
        });
    }

    @Test
    void localMethodChangeIsAudited() {
        FakePort port = new FakePort();
        List<SecurityAuditEvent> audit = new ArrayList<>();
        SecurityUserAdministrationService service =
                new SecurityUserAdministrationService(
                        port,
                        audit::add,
                        new BCryptPasswordEncoder(10)
                );

        service.setLocalAuthenticationEnabled(port.userId, false, "admin-subject");

        assertThat(audit).singleElement().satisfies(event ->
                assertThat(event.eventType())
                        .isEqualTo(SecurityAuditEventType.AUTH_METHOD_DISABLED)
        );
    }

    private static final class FakePort implements SecurityUserAdministrationPort {
        private final UUID userId = UUID.randomUUID();
        private String passwordHash;

        @Override public List<SecurityUserSummary> listUsers() { return List.of(); }
        @Override public SecurityUserDetail getUser(UUID userId) {
            return new SecurityUserDetail(
                    this.userId,
                    "user",
                    "user@sixpay.test",
                    SixpayUserAccountStatus.ACTIVE,
                    true,
                    false,
                    Set.of("ADMIN"),
                    Set.of("SCOPE_payment.read"),
                    List.of(),
                    List.of()
            );
        }
        @Override public void setLocalAuthenticationEnabled(UUID userId, boolean enabled) { }
        @Override public void resetLocalPassword(UUID userId, String bcryptHash) { passwordHash = bcryptHash; }
        @Override public void linkOidcIdentity(UUID userId, String provider, String providerSubject) { }
        @Override public void unlinkOidcIdentity(UUID userId, UUID identityId) { }
        @Override public void disableUser(UUID userId) { }
    }
}
