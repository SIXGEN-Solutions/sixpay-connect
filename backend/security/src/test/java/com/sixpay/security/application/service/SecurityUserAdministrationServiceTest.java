package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.PasswordReuseException;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.application.port.out.PasswordHistoryPort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecurityUserAdministrationServiceTest {

    private SecurityUserAdministrationPort administrationPort;
    private SecurityAuditPort auditPort;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicy passwordPolicy;
    private PasswordHistoryPort passwordHistoryPort;
    private SecurityUserAdministrationService service;

    @BeforeEach
    void setUp() {
        administrationPort = mock(SecurityUserAdministrationPort.class);
        auditPort = mock(SecurityAuditPort.class);
        passwordEncoder = mock(PasswordEncoder.class);
        passwordPolicy = new PasswordPolicy(12, 200, 5, 90);
        passwordHistoryPort = mock(PasswordHistoryPort.class);

        service = new SecurityUserAdministrationService(
                administrationPort,
                auditPort,
                passwordEncoder,
                passwordPolicy,
                passwordHistoryPort
        );
    }

    @Test
    void createsCanonicalUserWithNormalizedRoleAndPermission() {
        UUID userId = UUID.randomUUID();

        when(passwordEncoder.encode("Admin-dev-2026"))
                .thenReturn("$2a$12$hash");

        SecurityUserDetail expected = detail(
                userId,
                "admin",
                Set.of("ADMIN"),
                Set.of("payment.read")
        );

        when(administrationPort.createUser(
                eq(userId),
                eq("admin"),
                eq("admin@sixpay.local"),
                eq(Set.of("ADMIN")),
                eq(Set.of("payment.read")),
                eq(true),
                eq("$2a$12$hash")
        )).thenReturn(expected);

        SecurityUserDetail actual = service.createUser(
                new CreateSecurityUserCommand(
                        userId,
                        " ADMIN ",
                        " ADMIN@SIXPAY.LOCAL ",
                        Set.of("ROLE_ADMIN"),
                        Set.of("SCOPE_payment.read"),
                        true,
                        "Admin-dev-2026",
                        "seed"
                )
        );

        assertThat(actual).isSameAs(expected);

        var audit = ArgumentCaptor.forClass(
                com.sixpay.security.domain.administration.SecurityAuditEvent.class
        );

        verify(auditPort).record(audit.capture());
        assertThat(audit.getValue().eventType())
                .isEqualTo(SecurityAuditEventType.USER_CREATED);
        assertThat(audit.getValue().targetUserId()).isEqualTo(userId);
        verifyNoInteractions(passwordHistoryPort);
    }

    @Test
    void refusesLocalCreationWhenPasswordViolatesCentralPolicy() {
        assertThatThrownBy(() -> service.createUser(
                new CreateSecurityUserCommand(
                        UUID.randomUUID(),
                        "admin",
                        null,
                        Set.of("ADMIN"),
                        Set.of(),
                        true,
                        "short",
                        "seed"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must contain at least 12 characters");

        verifyNoInteractions(administrationPort);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(passwordHistoryPort);
    }

    @Test
    void refusesLocalPasswordResetWhenPasswordViolatesCentralPolicy() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() ->
                service.resetLocalPassword(
                        userId,
                        "short",
                        "admin"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must contain at least 12 characters");

        verify(administrationPort, never())
                .resetLocalPassword(any(), any());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(passwordHistoryPort);
    }

    @Test
    void refusesPasswordResetWhenCandidateMatchesCurrentPassword() {
        UUID userId = UUID.randomUUID();
        when(passwordHistoryPort.loadForPasswordReplacement(userId, 5))
                .thenReturn(new PasswordHistorySnapshot(
                        "current-hash",
                        List.of("old-1", "old-2")
                ));
        when(passwordEncoder.matches("Admin-dev-2026", "current-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.resetLocalPassword(
                userId,
                "Admin-dev-2026",
                "admin"
        ))
                .isInstanceOf(PasswordReuseException.class)
                .hasMessageContaining("must not reuse");

        verify(passwordEncoder, never()).encode(any());
        verify(passwordHistoryPort, never()).archiveReplacedPassword(
                any(), any(), any(), anyInt()
        );
        verify(administrationPort, never()).resetLocalPassword(any(), any());
    }

    @Test
    void refusesPasswordResetWhenCandidateMatchesRecentHistory() {
        UUID userId = UUID.randomUUID();
        when(passwordHistoryPort.loadForPasswordReplacement(userId, 5))
                .thenReturn(new PasswordHistorySnapshot(
                        "current-hash",
                        List.of("old-1", "old-2")
                ));
        when(passwordEncoder.matches("Admin-dev-2026", "current-hash"))
                .thenReturn(false);
        when(passwordEncoder.matches("Admin-dev-2026", "old-1"))
                .thenReturn(false);
        when(passwordEncoder.matches("Admin-dev-2026", "old-2"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.resetLocalPassword(
                userId,
                "Admin-dev-2026",
                "admin"
        ))
                .isInstanceOf(PasswordReuseException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(administrationPort, never()).resetLocalPassword(any(), any());
    }

    @Test
    void archivesCurrentHashBeforeSuccessfulPasswordReplacement() {
        UUID userId = UUID.randomUUID();
        when(passwordHistoryPort.loadForPasswordReplacement(userId, 5))
                .thenReturn(new PasswordHistorySnapshot(
                        "current-hash",
                        List.of("old-1", "old-2")
                ));
        when(passwordEncoder.matches(eq("New-admin-dev-2026"), anyString()))
                .thenReturn(false);
        when(passwordEncoder.encode("New-admin-dev-2026"))
                .thenReturn("new-hash");
        when(administrationPort.getUser(userId))
                .thenReturn(detail(
                        userId,
                        "admin",
                        Set.of("ADMIN"),
                        Set.of("payment.read")
                ));

        service.resetLocalPassword(
                userId,
                "New-admin-dev-2026",
                "admin"
        );

        var inOrder = inOrder(passwordHistoryPort, administrationPort, auditPort);
        inOrder.verify(passwordHistoryPort)
                .loadForPasswordReplacement(userId, 5);
        inOrder.verify(passwordHistoryPort)
                .archiveReplacedPassword(
                        eq(userId),
                        eq("current-hash"),
                        any(),
                        eq(5)
                );
        inOrder.verify(administrationPort)
                .resetLocalPassword(userId, "new-hash");
        inOrder.verify(auditPort).record(any());
        inOrder.verify(administrationPort).getUser(userId);
    }

    @Test
    void updatesCanonicalProfileAndAuthorization() {
        UUID userId = UUID.randomUUID();

        when(administrationPort.getUser(userId))
                .thenReturn(detail(
                        userId,
                        "ops-admin",
                        Set.of("ADMIN", "AUDITOR"),
                        Set.of("payment.read")
                ));

        SecurityUserDetail actual = service.updateUser(
                new UpdateSecurityUserCommand(
                        userId,
                        " Ops-Admin ",
                        "OPS@SIXPAY.LOCAL",
                        Set.of("ROLE_ADMIN", "AUDITOR"),
                        Set.of("SCOPE_payment.read"),
                        "admin"
                )
        );

        verify(administrationPort).updateUser(
                userId,
                "ops-admin",
                "ops@sixpay.local",
                Set.of("ADMIN", "AUDITOR"),
                Set.of("payment.read")
        );

        assertThat(actual.username()).isEqualTo("ops-admin");
    }

    @Test
    void rejectsUnknownRoleBeforePersistence() {
        assertThatThrownBy(() -> service.createUser(
                new CreateSecurityUserCommand(
                        UUID.randomUUID(),
                        "invalid-role-user",
                        null,
                        Set.of("ROOT"),
                        Set.of("payment.read"),
                        false,
                        null,
                        "admin"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown SIXPAY role");

        verifyNoInteractions(administrationPort);
    }

    @Test
    void rejectsUnknownPermissionBeforePersistence() {
        assertThatThrownBy(() -> service.createUser(
                new CreateSecurityUserCommand(
                        UUID.randomUUID(),
                        "invalid-permission-user",
                        null,
                        Set.of("AUDITOR"),
                        Set.of("everything.write"),
                        false,
                        null,
                        "admin"
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown SIXPAY permission");

        verifyNoInteractions(administrationPort);
    }

    @Test
    void deletesExistingUserAfterWritingAudit() {
        UUID userId = UUID.randomUUID();

        when(administrationPort.getUser(userId))
                .thenReturn(detail(
                        userId,
                        "auditor",
                        Set.of("AUDITOR"),
                        Set.of("payment.audit.read")
                ));

        service.deleteUser(userId, "admin");

        var inOrder = inOrder(administrationPort, auditPort);
        inOrder.verify(administrationPort).getUser(userId);
        inOrder.verify(auditPort).record(any());
        inOrder.verify(administrationPort).deleteUser(userId);
    }

    private static SecurityUserDetail detail(
            UUID id,
            String username,
            Set<String> roles,
            Set<String> permissions
    ) {
        return new SecurityUserDetail(
                id,
                username,
                username + "@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                roles,
                permissions,
                List.of(),
                List.of()
        );
    }
}
