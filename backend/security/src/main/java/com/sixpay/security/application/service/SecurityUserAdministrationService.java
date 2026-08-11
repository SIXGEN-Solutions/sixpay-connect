package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SecurityUserAdministrationService
        implements SecurityUserAdministrationUseCase {

    private final SecurityUserAdministrationPort administrationPort;
    private final SecurityAuditPort auditPort;
    private final PasswordEncoder passwordEncoder;

    public SecurityUserAdministrationService(
            SecurityUserAdministrationPort administrationPort,
            SecurityAuditPort auditPort,
            PasswordEncoder passwordEncoder
    ) {
        this.administrationPort = Objects.requireNonNull(administrationPort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityUserSummary> listUsers() {
        return administrationPort.listUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityUserDetail getUser(UUID userId) {
        return administrationPort.getUser(userId);
    }

    @Override
    @Transactional
    public SecurityUserDetail setLocalAuthenticationEnabled(
            UUID userId,
            boolean enabled,
            String actorSubject
    ) {
        administrationPort.setLocalAuthenticationEnabled(userId, enabled);
        auditPort.record(new SecurityAuditEvent(
                enabled
                        ? SecurityAuditEventType.AUTH_METHOD_ENABLED
                        : SecurityAuditEventType.AUTH_METHOD_DISABLED,
                actorSubject,
                userId,
                null,
                "SIXPAY",
                "LOCAL",
                Instant.now()
        ));
        return administrationPort.getUser(userId);
    }

    @Override
    @Transactional
    public SecurityUserDetail resetLocalPassword(
            UUID userId,
            String newPassword,
            String actorSubject
    ) {
        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException("New password must contain at least 12 characters");
        }

        administrationPort.resetLocalPassword(
                userId,
                passwordEncoder.encode(newPassword)
        );

        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.PASSWORD_RESET,
                actorSubject,
                userId,
                null,
                "SIXPAY",
                null,
                Instant.now()
        ));

        return administrationPort.getUser(userId);
    }

    @Override
    @Transactional
    public SecurityUserDetail linkOidcIdentity(
            UUID userId,
            String provider,
            String providerSubject,
            String actorSubject
    ) {
        administrationPort.linkOidcIdentity(userId, provider, providerSubject);
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.IDENTITY_LINKED,
                actorSubject,
                userId,
                null,
                provider,
                null,
                Instant.now()
        ));
        return administrationPort.getUser(userId);
    }

    @Override
    @Transactional
    public SecurityUserDetail unlinkOidcIdentity(
            UUID userId,
            UUID identityId,
            String actorSubject
    ) {
        administrationPort.unlinkOidcIdentity(userId, identityId);
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.IDENTITY_UNLINKED,
                actorSubject,
                userId,
                null,
                null,
                null,
                Instant.now()
        ));
        return administrationPort.getUser(userId);
    }

    @Override
    @Transactional
    public SecurityUserDetail disableUser(
            UUID userId,
            String actorSubject
    ) {
        administrationPort.disableUser(userId);
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.USER_DISABLED,
                actorSubject,
                userId,
                null,
                null,
                null,
                Instant.now()
        ));
        return administrationPort.getUser(userId);
    }
}
