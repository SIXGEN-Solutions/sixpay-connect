package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.exception.LocalAuthenticationFailedException;
import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.LocalLoginCommand;
import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.LoadAuthenticationUserPort;
import com.sixpay.security.application.port.out.PasswordVerificationPort;
import com.sixpay.security.application.port.out.SaveAuthenticationUserStatePort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditOutcome;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditType;
import com.sixpay.security.domain.authentication.LocalAuthenticationUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class LocalAuthenticationService
        implements AuthenticateLocalUserUseCase {

    private final LoadAuthenticationUserPort loadUserPort;
    private final SaveAuthenticationUserStatePort saveUserStatePort;
    private final PasswordVerificationPort passwordVerificationPort;
    private final AuthenticationAuditPort auditPort;
    private final TimeProvider timeProvider;
    private final int maximumFailedAttempts;
    private final Duration lockDuration;

    public LocalAuthenticationService(
            LoadAuthenticationUserPort loadUserPort,
            SaveAuthenticationUserStatePort saveUserStatePort,
            PasswordVerificationPort passwordVerificationPort,
            AuthenticationAuditPort auditPort,
            TimeProvider timeProvider,
            int maximumFailedAttempts,
            Duration lockDuration
    ) {
        this.loadUserPort = Objects.requireNonNull(loadUserPort);
        this.saveUserStatePort = Objects.requireNonNull(saveUserStatePort);
        this.passwordVerificationPort = Objects.requireNonNull(passwordVerificationPort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.timeProvider = Objects.requireNonNull(timeProvider);

        if (maximumFailedAttempts < 1) {
            throw new IllegalArgumentException("Maximum failed attempts must be positive");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("Lock duration must be positive");
        }

        this.maximumFailedAttempts = maximumFailedAttempts;
        this.lockDuration = lockDuration;
    }

    @Override
    @Transactional(noRollbackFor = LocalAuthenticationFailedException.class)
    public AuthenticatedUser authenticate(LocalLoginCommand command) {
        Objects.requireNonNull(command, "Local login command must not be null");

        String normalizedUsername = normalizeUsername(command.username());
        Instant now = timeProvider.now();

        LocalAuthenticationUser loadedUser =
                loadUserPort.loadForAuthentication(normalizedUsername)
                        .orElse(null);

        if (loadedUser == null) {
            passwordVerificationPort.performDummyVerification(command.password());
            recordFailure(null, normalizedUsername, now);
            throw new LocalAuthenticationFailedException();
        }

        if (!loadedUser.active()) {
            passwordVerificationPort.performDummyVerification(command.password());
            recordFailure(loadedUser.subject(), loadedUser.username(), now);
            throw new LocalAuthenticationFailedException();
        }

        LocalAuthenticationUser effectiveUser =
                loadedUser.unlockIfExpired(now);

        if (effectiveUser.lockedAt(now)) {
            passwordVerificationPort.performDummyVerification(command.password());
            recordFailure(effectiveUser.subject(), effectiveUser.username(), now);
            throw new LocalAuthenticationFailedException();
        }

        if (!passwordVerificationPort.matches(
                command.password(),
                effectiveUser.passwordHash()
        )) {
            LocalAuthenticationUser failedUser =
                    effectiveUser.authenticationFailed(
                            now,
                            maximumFailedAttempts,
                            lockDuration
                    );
            saveUserStatePort.saveAuthenticationState(failedUser);
            recordFailure(failedUser.subject(), failedUser.username(), now);
            throw new LocalAuthenticationFailedException();
        }

        LocalAuthenticationUser authenticatedUser =
                effectiveUser.authenticationSucceeded(now);

        saveUserStatePort.saveAuthenticationState(authenticatedUser);

        auditPort.record(
                new LocalAuthenticationAuditEvent(
                        LocalAuthenticationAuditType.LOGIN,
                        authenticatedUser.subject(),
                        authenticatedUser.username(),
                        LocalAuthenticationAuditOutcome.SUCCESS,
                        now
                )
        );

        return new AuthenticatedUser(
                authenticatedUser.subject(),
                authenticatedUser.username(),
                authenticatedUser.authorities()
        );
    }

    static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private void recordFailure(
            String subject,
            String username,
            Instant occurredAt
    ) {
        auditPort.record(
                new LocalAuthenticationAuditEvent(
                        LocalAuthenticationAuditType.LOGIN,
                        subject,
                        username,
                        LocalAuthenticationAuditOutcome.FAILURE,
                        occurredAt
                )
        );
    }
}
