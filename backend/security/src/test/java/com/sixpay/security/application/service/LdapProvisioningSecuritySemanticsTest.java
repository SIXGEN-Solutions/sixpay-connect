package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.CreateSecurityUserCommand;
import com.sixpay.security.application.port.output.PasswordHistoryPort;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.application.port.output.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LdapProvisioningSecuritySemanticsTest {

    @Test
    void canonicalLdapUserCreationDoesNotCreateLocalCredentialAndEmitsUserCreatedAudit() {
        SecurityUserAdministrationPort administrationPort =
                mock(SecurityUserAdministrationPort.class);
        SecurityAuditPort auditPort =
                mock(SecurityAuditPort.class);
        PasswordEncoder passwordEncoder =
                mock(PasswordEncoder.class);
        PasswordHistoryPort passwordHistoryPort =
                mock(PasswordHistoryPort.class);

        PasswordPolicy passwordPolicy =
                new PasswordPolicy(
                        12,
                        128,
                        5,
                        90
                );

        UUID userId =
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

        when(administrationPort.createUser(
                eq(userId),
                eq("jane.doe"),
                eq("jane.doe@example.test"),
                eq(Set.of("ADMIN")),
                eq(Set.of("payment.read")),
                eq(false),
                isNull()
        )).thenReturn(mock(SecurityUserDetail.class));

        SecurityUserAdministrationService service =
                new SecurityUserAdministrationService(
                        administrationPort,
                        auditPort,
                        passwordEncoder,
                        passwordPolicy,
                        passwordHistoryPort
                );

        service.createUser(
                new CreateSecurityUserCommand(
                        userId,
                        "jane.doe",
                        "jane.doe@example.test",
                        Set.of("ADMIN"),
                        Set.of("payment.read"),
                        false,
                        null,
                        "admin-subject"
                )
        );

        verify(passwordEncoder, never()).encode(anyString());

        verify(administrationPort).createUser(
                userId,
                "jane.doe",
                "jane.doe@example.test",
                Set.of("ADMIN"),
                Set.of("payment.read"),
                false,
                null
        );

        ArgumentCaptor<SecurityAuditEvent> audit =
                ArgumentCaptor.forClass(SecurityAuditEvent.class);

        verify(auditPort).record(audit.capture());

        assertThat(audit.getValue().eventType())
                .isEqualTo(SecurityAuditEventType.USER_CREATED);
        assertThat(audit.getValue().actorSubject())
                .isEqualTo("admin-subject");
        assertThat(audit.getValue().detail())
                .isEqualTo("CANONICAL_ONLY");
    }

    @Test
    void invalidRoleIsRejectedBeforePersistence() {
        SecurityUserAdministrationPort administrationPort =
                mock(SecurityUserAdministrationPort.class);

        SecurityUserAdministrationService service =
                service(administrationPort);

        assertThatThrownBy(
                () -> service.createUser(
                        new CreateSecurityUserCommand(
                                UUID.randomUUID(),
                                "jane.doe",
                                null,
                                Set.of("NOT_A_SIXPAY_ROLE"),
                                Set.of(),
                                false,
                                null,
                                "admin"
                        )
                )
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(administrationPort);
    }

    @Test
    void invalidPermissionIsRejectedBeforePersistence() {
        SecurityUserAdministrationPort administrationPort =
                mock(SecurityUserAdministrationPort.class);

        SecurityUserAdministrationService service =
                service(administrationPort);

        assertThatThrownBy(
                () -> service.createUser(
                        new CreateSecurityUserCommand(
                                UUID.randomUUID(),
                                "jane.doe",
                                null,
                                Set.of(),
                                Set.of("not.a.real.permission"),
                                false,
                                null,
                                "admin"
                        )
                )
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(administrationPort);
    }

    private static SecurityUserAdministrationService service(
            SecurityUserAdministrationPort administrationPort
    ) {
        return new SecurityUserAdministrationService(
                administrationPort,
                mock(SecurityAuditPort.class),
                mock(PasswordEncoder.class),
                mock(PasswordPolicy.class),
                mock(PasswordHistoryPort.class)
        );
    }
}
