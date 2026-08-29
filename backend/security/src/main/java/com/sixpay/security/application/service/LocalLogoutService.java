package com.sixpay.security.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.security.application.port.input.LogoutUseCase;
import com.sixpay.security.application.port.output.AuthenticationAuditPort;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditOutcome;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditType;

import java.util.Objects;

public final class LocalLogoutService implements LogoutUseCase {

    private final AuthenticationAuditPort auditPort;
    private final TimeProvider timeProvider;

    public LocalLogoutService(
            AuthenticationAuditPort auditPort,
            TimeProvider timeProvider
    ) {
        this.auditPort = Objects.requireNonNull(auditPort);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public void logout(AuthenticatedUser currentUser) {
        Objects.requireNonNull(currentUser, "Current user must not be null");

        auditPort.record(
                new LocalAuthenticationAuditEvent(
                        LocalAuthenticationAuditType.LOGOUT,
                        currentUser.subject(),
                        currentUser.username(),
                        LocalAuthenticationAuditOutcome.SUCCESS,
                        timeProvider.now()
                )
        );
    }
}
