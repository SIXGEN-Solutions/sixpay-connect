package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.PasswordReuseException;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.out.PasswordHistoryPort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
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

class SecurityUserAdministrationPasswordResetTest {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private SecurityUserAdministrationPort administrationPort;
    private SecurityAuditPort auditPort;
    private PasswordEncoder passwordEncoder;
    private PasswordHistoryPort passwordHistoryPort;
    private SecurityUserAdministrationService service;

    @BeforeEach
    void setUp() {
        administrationPort =
                mock(
                        SecurityUserAdministrationPort.class
                );

        auditPort =
                mock(
                        SecurityAuditPort.class
                );

        passwordEncoder =
                mock(
                        PasswordEncoder.class
                );

        passwordHistoryPort =
                mock(
                        PasswordHistoryPort.class
                );

        service =
                new SecurityUserAdministrationService(
                        administrationPort,
                        auditPort,
                        passwordEncoder,
                        new PasswordPolicy(
                                12,
                                200,
                                5,
                                90
                        ),
                        passwordHistoryPort
                );
    }

    @Test
    void administrativeResetArchivesCurrentHashWritesTemporaryCredentialAndAudits() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        new PasswordHistorySnapshot(
                                "current-hash",
                                List.of(
                                        "old-1",
                                        "old-2"
                                )
                        )
                );

        when(
                passwordEncoder.matches(
                        eq("Temporary-password-2027"),
                        anyString()
                )
        )
                .thenReturn(false);

        when(
                passwordEncoder.encode(
                        "Temporary-password-2027"
                )
        )
                .thenReturn(
                        "temporary-hash"
                );

        when(
                administrationPort.getUser(
                        USER_ID
                )
        )
                .thenReturn(
                        detail()
                );

        SecurityUserDetail result =
                service.resetLocalPassword(
                        USER_ID,
                        "Temporary-password-2027",
                        "admin-subject"
                );

        var inOrder =
                inOrder(
                        passwordHistoryPort,
                        administrationPort,
                        auditPort
                );

        inOrder.verify(
                passwordHistoryPort
        )
                .loadForPasswordReplacement(
                        USER_ID,
                        5
                );

        inOrder.verify(
                passwordHistoryPort
        )
                .archiveReplacedPassword(
                        eq(USER_ID),
                        eq("current-hash"),
                        any(),
                        eq(5)
                );

        inOrder.verify(
                administrationPort
        )
                .resetLocalPassword(
                        USER_ID,
                        "temporary-hash"
                );

        ArgumentCaptor<SecurityAuditEvent> audit =
                ArgumentCaptor.forClass(
                        SecurityAuditEvent.class
                );

        inOrder.verify(
                auditPort
        )
                .record(
                        audit.capture()
                );

        inOrder.verify(
                administrationPort
        )
                .getUser(USER_ID);

        assertThat(result)
                .isNotNull();

        assertThat(
                audit.getValue()
                        .eventType()
        )
                .isEqualTo(
                        SecurityAuditEventType.PASSWORD_RESET
                );

        assertThat(
                audit.getValue()
                        .actorSubject()
        )
                .isEqualTo(
                        "admin-subject"
                );

        assertThat(
                audit.getValue()
                        .targetUserId()
        )
                .isEqualTo(USER_ID);

        assertThat(
                audit.getValue()
                        .provider()
        )
                .isEqualTo("SIXPAY");
    }

    @Test
    void resetRejectsReuseBeforeEncodingOrPersistence() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        new PasswordHistorySnapshot(
                                "current-hash",
                                List.of(
                                        "old-hash"
                                )
                        )
                );

        when(
                passwordEncoder.matches(
                        "Temporary-password-2027",
                        "current-hash"
                )
        )
                .thenReturn(false);

        when(
                passwordEncoder.matches(
                        "Temporary-password-2027",
                        "old-hash"
                )
        )
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.resetLocalPassword(
                        USER_ID,
                        "Temporary-password-2027",
                        "admin-subject"
                )
        )
                .isInstanceOf(
                        PasswordReuseException.class
                );

        verify(
                passwordEncoder,
                never()
        )
                .encode(anyString());

        verify(
                passwordHistoryPort,
                never()
        )
                .archiveReplacedPassword(
                        any(),
                        any(),
                        any(),
                        anyInt()
                );

        verify(
                administrationPort,
                never()
        )
                .resetLocalPassword(
                        any(),
                        any()
                );

        verifyNoInteractions(
                auditPort
        );
    }

    private static SecurityUserDetail detail() {
        return new SecurityUserDetail(
                USER_ID,
                "manager",
                "manager@sixpay.local",
                SixpayUserAccountStatus.ACTIVE,
                true,
                false,
                Set.of("MANAGER"),
                Set.of("payment.read"),
                List.of(),
                List.of()
        );
    }
}
