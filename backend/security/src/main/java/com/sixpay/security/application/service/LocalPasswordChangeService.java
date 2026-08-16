package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.exception.CurrentPasswordMismatchException;
import com.sixpay.security.application.exception.PasswordReuseException;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.port.in.ChangeLocalPasswordCommand;
import com.sixpay.security.application.port.in.ChangeLocalPasswordUseCase;
import com.sixpay.security.application.port.out.ChangeLocalCredentialPort;
import com.sixpay.security.application.port.out.PasswordHistoryPort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * User-owned LOCAL password change.
 *
 * <p>The transaction keeps current-password verification, anti-reuse
 * validation, history archival, credential replacement and audit atomic.</p>
 */
public class LocalPasswordChangeService
        implements ChangeLocalPasswordUseCase {

    private final PasswordHistoryPort passwordHistoryPort;
    private final ChangeLocalCredentialPort credentialPort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SecurityAuditPort auditPort;
    private final TimeProvider timeProvider;

    public LocalPasswordChangeService(
            PasswordHistoryPort passwordHistoryPort,
            ChangeLocalCredentialPort credentialPort,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            SecurityAuditPort auditPort,
            TimeProvider timeProvider
    ) {
        this.passwordHistoryPort =
                Objects.requireNonNull(passwordHistoryPort);
        this.credentialPort =
                Objects.requireNonNull(credentialPort);
        this.passwordEncoder =
                Objects.requireNonNull(passwordEncoder);
        this.passwordPolicy =
                Objects.requireNonNull(passwordPolicy);
        this.auditPort =
                Objects.requireNonNull(auditPort);
        this.timeProvider =
                Objects.requireNonNull(timeProvider);
    }

    @Override
    @Transactional
    public void changePassword(
            ChangeLocalPasswordCommand command
    ) {
        Objects.requireNonNull(
                command,
                "Change local password command must not be null"
        );

        /*
         * DA-10.3's history boundary locks the current LOCAL credential.
         * Keep that lock for the whole replacement transaction.
         */
        PasswordHistorySnapshot history =
                passwordHistoryPort
                        .loadForPasswordReplacement(
                                command.userId(),
                                passwordPolicy.historySize()
                        );

        verifyCurrentPassword(
                command.currentPassword(),
                history.currentPasswordHash()
        );

        /*
         * Required sequence:
         * current-password proof -> policy -> history -> replacement.
         */
        passwordPolicy.validate(
                command.newPassword()
        );

        rejectPasswordReuse(
                command.newPassword(),
                history
        );

        String newPasswordHash =
                passwordEncoder.encode(
                        command.newPassword()
                );

        Instant changedAt =
                timeProvider.now();

        /*
         * Archive the credential being replaced. If the credential write or
         * audit fails, the surrounding transaction rolls this insert back.
         */
        passwordHistoryPort
                .archiveReplacedPassword(
                        command.userId(),
                        history.currentPasswordHash(),
                        changedAt,
                        passwordPolicy.historySize()
                );

        /*
         * User-owned change is deliberately different from ADMIN reset:
         * entity.changePassword(...) clears mustChangePassword and computes
         * normal expiration from PasswordPolicy.
         */
        credentialPort.changePassword(
                command.userId(),
                newPasswordHash,
                changedAt,
                passwordPolicy
        );

        auditPort.record(
                new SecurityAuditEvent(
                        SecurityAuditEventType.PASSWORD_CHANGED,
                        command.actorSubject(),
                        command.userId(),
                        null,
                        "SIXPAY",
                        "LOCAL",
                        changedAt
                )
        );
    }

    private void verifyCurrentPassword(
            String rawCurrentPassword,
            String currentPasswordHash
    ) {
        if (!passwordEncoder.matches(
                rawCurrentPassword,
                currentPasswordHash
        )) {
            throw new CurrentPasswordMismatchException();
        }
    }

    private void rejectPasswordReuse(
            String rawNewPassword,
            PasswordHistorySnapshot history
    ) {
        if (passwordEncoder.matches(
                rawNewPassword,
                history.currentPasswordHash()
        )) {
            throw new PasswordReuseException();
        }

        boolean matchesHistory =
                history.recentPasswordHashes()
                        .stream()
                        .anyMatch(hash ->
                                passwordEncoder.matches(
                                        rawNewPassword,
                                        hash
                                )
                        );

        if (matchesHistory) {
            throw new PasswordReuseException();
        }
    }
}
