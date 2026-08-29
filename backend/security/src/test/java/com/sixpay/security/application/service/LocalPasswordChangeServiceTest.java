package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.exception.CurrentPasswordMismatchException;
import com.sixpay.security.application.exception.PasswordReuseException;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.port.input.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.output.ChangeLocalCredentialPort;
import com.sixpay.security.application.port.output.PasswordHistoryPort;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LocalPasswordChangeServiceTest {

    private static final UUID USER_ID =
            UUID.fromString(
                    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
            );

    private static final Instant NOW =
            Instant.parse(
                    "2026-08-15T22:00:00Z"
            );

    private PasswordHistoryPort passwordHistoryPort;
    private ChangeLocalCredentialPort credentialPort;
    private PasswordEncoder passwordEncoder;
    private SecurityAuditPort auditPort;
    private TimeProvider timeProvider;
    private PasswordPolicy passwordPolicy;
    private LocalPasswordChangeService service;

    @BeforeEach
    void setUp() {
        passwordHistoryPort =
                mock(PasswordHistoryPort.class);
        credentialPort =
                mock(ChangeLocalCredentialPort.class);
        passwordEncoder =
                mock(PasswordEncoder.class);
        auditPort =
                mock(SecurityAuditPort.class);
        timeProvider =
                mock(TimeProvider.class);

        passwordPolicy =
                new PasswordPolicy(
                        12,
                        200,
                        5,
                        90
                );

        service =
                new LocalPasswordChangeService(
                        passwordHistoryPort,
                        credentialPort,
                        passwordEncoder,
                        passwordPolicy,
                        auditPort,
                        timeProvider
                );
    }

    @Test
    void rejectsChangeWhenCurrentPasswordIsInvalid() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        snapshot()
                );

        when(
                passwordEncoder.matches(
                        "wrong-current",
                        "current-hash"
                )
        )
                .thenReturn(false);

        assertThatThrownBy(() ->
                service.changePassword(
                        command(
                                "wrong-current",
                                "New-password-2026"
                        )
                )
        )
                .isInstanceOf(
                        CurrentPasswordMismatchException.class
                );

        verify(
                passwordEncoder,
                never()
        )
                .encode(anyString());

        verifyNoInteractions(
                credentialPort
        );

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

        verifyNoInteractions(
                auditPort
        );
    }

    @Test
    void validatesPolicyOnlyAfterCurrentPasswordWasProved() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        snapshot()
                );

        when(
                passwordEncoder.matches(
                        "Current-password-2026",
                        "current-hash"
                )
        )
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.changePassword(
                        command(
                                "Current-password-2026",
                                "short"
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Password must contain at least 12 characters"
                );

        verify(
                passwordEncoder,
                never()
        )
                .encode(anyString());

        verifyNoInteractions(
                credentialPort
        );
    }

    @Test
    void rejectsNewPasswordWhenItMatchesCurrentPassword() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        snapshot()
                );

        when(
                passwordEncoder.matches(
                        "Current-password-2026",
                        "current-hash"
                )
        )
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.changePassword(
                        command(
                                "Current-password-2026",
                                "Current-password-2026"
                        )
                )
        )
                .isInstanceOf(
                        PasswordReuseException.class
                );

        verifyNoInteractions(
                credentialPort
        );
    }

    @Test
    void rejectsNewPasswordWhenItMatchesRecentHistory() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        snapshot()
                );

        when(
                passwordEncoder.matches(
                        "Current-password-2026",
                        "current-hash"
                )
        )
                .thenReturn(true);

        when(
                passwordEncoder.matches(
                        "Historical-password-2026",
                        "current-hash"
                )
        )
                .thenReturn(false);

        when(
                passwordEncoder.matches(
                        "Historical-password-2026",
                        "history-1"
                )
        )
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.changePassword(
                        command(
                                "Current-password-2026",
                                "Historical-password-2026"
                        )
                )
        )
                .isInstanceOf(
                        PasswordReuseException.class
                );

        verifyNoInteractions(
                credentialPort
        );
    }

    @Test
    void changesPasswordAndWritesPasswordChangedAudit() {
        when(
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                USER_ID,
                                5
                        )
        )
                .thenReturn(
                        snapshot()
                );

        when(
                passwordEncoder.matches(
                        "Current-password-2026",
                        "current-hash"
                )
        )
                .thenReturn(true);

        when(
                passwordEncoder.matches(
                        eq("Brand-new-password-2026"),
                        anyString()
                )
        )
                .thenReturn(false);

        when(
                passwordEncoder.encode(
                        "Brand-new-password-2026"
                )
        )
                .thenReturn(
                        "new-hash"
                );

        when(timeProvider.now())
                .thenReturn(NOW);

        service.changePassword(
                command(
                        "Current-password-2026",
                        "Brand-new-password-2026"
                )
        );

        var inOrder =
                inOrder(
                        passwordHistoryPort,
                        credentialPort,
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
                        USER_ID,
                        "current-hash",
                        NOW,
                        5
                );

        inOrder.verify(
                credentialPort
        )
                .changePassword(
                        USER_ID,
                        "new-hash",
                        NOW,
                        passwordPolicy
                );

        ArgumentCaptor<SecurityAuditEvent> audit =
                ArgumentCaptor.forClass(
                        SecurityAuditEvent.class
                );

        inOrder.verify(auditPort)
                .record(
                        audit.capture()
                );

        assertThat(
                audit.getValue()
                        .eventType()
        )
                .isEqualTo(
                        SecurityAuditEventType.PASSWORD_CHANGED
                );

        assertThat(
                audit.getValue()
                        .actorSubject()
        )
                .isEqualTo(
                        USER_ID.toString()
                );

        assertThat(
                audit.getValue()
                        .targetUserId()
        )
                .isEqualTo(USER_ID);
    }

    private static PasswordHistorySnapshot snapshot() {
        return new PasswordHistorySnapshot(
                "current-hash",
                List.of(
                        "history-1",
                        "history-2"
                )
        );
    }

    private static ChangeLocalPasswordCommand command(
            String currentPassword,
            String newPassword
    ) {
        return new ChangeLocalPasswordCommand(
                USER_ID,
                USER_ID.toString(),
                currentPassword,
                newPassword
        );
    }
}
