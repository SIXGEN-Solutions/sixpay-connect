package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.PasswordReuseException;
import com.sixpay.security.application.model.PasswordHistorySnapshot;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.model.SecurityUserSummary;
import com.sixpay.security.application.port.in.CreateSecurityUserCommand;
import com.sixpay.security.application.port.in.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.in.UpdateSecurityUserCommand;
import com.sixpay.security.application.port.out.PasswordHistoryPort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.port.out.SecurityUserAdministrationPort;
import com.sixpay.security.authorization.SixpayPermission;
import com.sixpay.security.authorization.SixpayRole;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class SecurityUserAdministrationService implements SecurityUserAdministrationUseCase {
    private final SecurityUserAdministrationPort administrationPort;
    private final SecurityAuditPort auditPort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final PasswordHistoryPort passwordHistoryPort;

    public SecurityUserAdministrationService(
            SecurityUserAdministrationPort administrationPort,
            SecurityAuditPort auditPort,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            PasswordHistoryPort passwordHistoryPort
    ) {
        this.administrationPort = Objects.requireNonNull(administrationPort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.passwordPolicy = Objects.requireNonNull(passwordPolicy);
        this.passwordHistoryPort = Objects.requireNonNull(passwordHistoryPort);
    }

    @Override @Transactional
    public SecurityUserDetail createUser(CreateSecurityUserCommand command) {
        Objects.requireNonNull(command, "Create security user command must not be null");
        UUID userId = Objects.requireNonNull(command.userId(), "SIXPAY user id must not be null");
        String username = normalizeUsername(command.username());
        String email = normalizeEmail(command.email());
        Set<String> roles = normalizeRoles(command.roles());
        Set<String> permissions = normalizePermissions(command.permissions());
        String passwordHash = null;
        if (command.localAuthenticationEnabled()) {
            passwordPolicy.validate(command.initialPassword());
            passwordHash = passwordEncoder.encode(command.initialPassword());
        }
        SecurityUserDetail created = administrationPort.createUser(
                userId, username, email, roles, permissions,
                command.localAuthenticationEnabled(), passwordHash
        );
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.USER_CREATED, command.actorSubject(),
                userId, username, "SIXPAY",
                command.localAuthenticationEnabled() ? "LOCAL_PROVISIONED" : "CANONICAL_ONLY",
                Instant.now()
        ));
        return created;
    }

    @Override @Transactional(readOnly = true)
    public List<SecurityUserSummary> listUsers() { return administrationPort.listUsers(); }

    @Override @Transactional(readOnly = true)
    public SecurityUserDetail getUser(UUID userId) { return administrationPort.getUser(userId); }

    @Override @Transactional
    public SecurityUserDetail updateUser(UpdateSecurityUserCommand command) {
        Objects.requireNonNull(command, "Update security user command must not be null");
        UUID userId = Objects.requireNonNull(command.userId(), "SIXPAY user id must not be null");
        String username = normalizeUsername(command.username());
        String email = normalizeEmail(command.email());
        administrationPort.updateUser(userId, username, email,
                normalizeRoles(command.roles()), normalizePermissions(command.permissions()));
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.USER_UPDATED, command.actorSubject(), userId,
                username, "SIXPAY", null, Instant.now()
        ));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail enableUser(UUID userId, String actorSubject) {
        administrationPort.enableUser(userId);
        auditPort.record(new SecurityAuditEvent(SecurityAuditEventType.USER_ENABLED, actorSubject,
                userId, null, "SIXPAY", null, Instant.now()));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail setLocalAuthenticationEnabled(UUID userId, boolean enabled, String actorSubject) {
        administrationPort.setLocalAuthenticationEnabled(userId, enabled);
        auditPort.record(new SecurityAuditEvent(
                enabled ? SecurityAuditEventType.AUTH_METHOD_ENABLED : SecurityAuditEventType.AUTH_METHOD_DISABLED,
                actorSubject, userId, null, "SIXPAY", "LOCAL", Instant.now()
        ));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail resetLocalPassword(UUID userId, String newPassword, String actorSubject) {
        passwordPolicy.validate(newPassword);
        PasswordHistorySnapshot history = passwordHistoryPort.loadForPasswordReplacement(
                userId, passwordPolicy.historySize()
        );
        rejectPasswordReuse(newPassword, history);
        String newPasswordHash = passwordEncoder.encode(newPassword);
        Instant changedAt = Instant.now();
        passwordHistoryPort.archiveReplacedPassword(
                userId, history.currentPasswordHash(), changedAt, passwordPolicy.historySize()
        );
        administrationPort.resetLocalPassword(userId, newPasswordHash);
        auditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.PASSWORD_RESET, actorSubject, userId,
                null, "SIXPAY", null, changedAt
        ));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail linkOidcIdentity(UUID userId, String provider, String providerSubject, String actorSubject) {
        administrationPort.linkOidcIdentity(userId, provider, providerSubject);
        auditPort.record(new SecurityAuditEvent(SecurityAuditEventType.IDENTITY_LINKED, actorSubject,
                userId, null, provider, null, Instant.now()));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail unlinkOidcIdentity(UUID userId, UUID identityId, String actorSubject) {
        administrationPort.unlinkOidcIdentity(userId, identityId);
        auditPort.record(new SecurityAuditEvent(SecurityAuditEventType.IDENTITY_UNLINKED, actorSubject,
                userId, null, null, null, Instant.now()));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public SecurityUserDetail disableUser(UUID userId, String actorSubject) {
        administrationPort.disableUser(userId);
        auditPort.record(new SecurityAuditEvent(SecurityAuditEventType.USER_DISABLED, actorSubject,
                userId, null, null, null, Instant.now()));
        return administrationPort.getUser(userId);
    }

    @Override @Transactional
    public void deleteUser(UUID userId, String actorSubject) {
        SecurityUserDetail existing = administrationPort.getUser(userId);
        auditPort.record(new SecurityAuditEvent(SecurityAuditEventType.USER_DELETED, actorSubject,
                userId, existing.username(), "SIXPAY", null, Instant.now()));
        administrationPort.deleteUser(userId);
    }

    private void rejectPasswordReuse(String rawPassword, PasswordHistorySnapshot history) {
        if (passwordEncoder.matches(rawPassword, history.currentPasswordHash())) {
            throw new PasswordReuseException();
        }
        boolean matchesHistory = history.recentPasswordHashes().stream()
                .anyMatch(hash -> passwordEncoder.matches(rawPassword, hash));
        if (matchesHistory) {
            throw new PasswordReuseException();
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username must not be blank");
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 150) throw new IllegalArgumentException("Username must contain at most 150 characters");
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 320) throw new IllegalArgumentException("Email must contain at most 320 characters");
        return normalized;
    }

    private static Set<String> normalizeRoles(Set<String> roles) {
        if (roles == null) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) continue;
            String value = role.trim().toUpperCase(Locale.ROOT);
            if (value.startsWith("ROLE_")) value = value.substring("ROLE_".length());
            if (value.isBlank()) continue;
            try { normalized.add(SixpayRole.valueOf(value).name()); }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown SIXPAY role: " + role, exception);
            }
        }
        return Set.copyOf(normalized);
    }

    private static Set<String> normalizePermissions(Set<String> permissions) {
        if (permissions == null) return Set.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) continue;
            normalized.add(SixpayPermission.fromValue(permission).value());
        }
        return Set.copyOf(normalized);
    }
}
