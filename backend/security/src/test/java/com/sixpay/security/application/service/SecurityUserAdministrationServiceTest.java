package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
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
    private SecurityUserAdministrationService service;

    @BeforeEach
    void setUp() {
        administrationPort = mock(SecurityUserAdministrationPort.class);
        auditPort = mock(SecurityAuditPort.class);
        passwordEncoder = mock(PasswordEncoder.class);

        service = new SecurityUserAdministrationService(
                administrationPort,
                auditPort,
                passwordEncoder
        );
    }

    @Test
    void createsCanonicalUserWithLocalCredentialsAndAuditsCreation() {
        UUID userId = UUID.randomUUID();

        when(passwordEncoder.encode("Admin-dev-2026"))
                .thenReturn("$2a$12$hash");

        SecurityUserDetail expected = detail(
                userId,
                "admin",
                Set.of("ADMIN")
        );

        when(administrationPort.createUser(
                eq(userId),
                eq("admin"),
                eq("admin@sixpay.local"),
                eq(Set.of("ADMIN")),
                eq(Set.of()),
                eq(true),
                eq("$2a$12$hash")
        )).thenReturn(expected);

        SecurityUserDetail actual = service.createUser(
                new CreateSecurityUserCommand(
                        userId,
                        " ADMIN ",
                        " ADMIN@SIXPAY.LOCAL ",
                        Set.of("ROLE_ADMIN"),
                        Set.of(),
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
        assertThat(audit.getValue().targetUserId())
                .isEqualTo(userId);
    }

    @Test
    void refusesLocalCreationWithoutStrongEnoughPassword() {
        assertThatThrownBy(() ->
                service.createUser(
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
                )
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(administrationPort);
    }

    @Test
    void updatesCanonicalProfileAndAuthorization() {
        UUID userId = UUID.randomUUID();

        when(administrationPort.getUser(userId))
                .thenReturn(detail(
                        userId,
                        "ops-admin",
                        Set.of("ADMIN", "AUDITOR")
                ));

        SecurityUserDetail actual = service.updateUser(
                new UpdateSecurityUserCommand(
                        userId,
                        " Ops-Admin ",
                        "OPS@SIXPAY.LOCAL",
                        Set.of("ROLE_ADMIN", "AUDITOR"),
                        Set.of("payment.read"),
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
    void deletesExistingUserAfterWritingAudit() {
        UUID userId = UUID.randomUUID();

        when(administrationPort.getUser(userId))
                .thenReturn(detail(
                        userId,
                        "auditor",
                        Set.of("AUDITOR")
                ));

        service.deleteUser(userId, "admin");

        var inOrder = inOrder(
                administrationPort,
                auditPort
        );
        inOrder.verify(administrationPort).getUser(userId);
        inOrder.verify(auditPort).record(any());
        inOrder.verify(administrationPort).deleteUser(userId);
    }

    private static SecurityUserDetail detail(
            UUID id,
            String username,
            Set<String> roles
    ) {
        return new SecurityUserDetail(
                id,
                username,
                username + "@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                roles,
                Set.of(),
                List.of(),
                List.of()
        );
    }
}
