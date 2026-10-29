package com.sixpay.security.application.service;

import com.sixpay.security.application.port.input.LdapProvisioningConflictException;
import com.sixpay.security.application.port.input.LdapUserNotProvisionableException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.input.DirectoryUserLookupUseCase;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserCommand;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserUseCase;
import com.sixpay.security.application.port.output.LdapSecurityUserProvisioningPort;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ProvisionLdapSecurityUserService
        implements ProvisionLdapSecurityUserUseCase {

    private final DirectoryUserLookupUseCase directoryLookup;
    private final LdapSecurityUserProvisioningPort provisioningPort;

    public ProvisionLdapSecurityUserService(
            DirectoryUserLookupUseCase directoryLookup,
            LdapSecurityUserProvisioningPort provisioningPort
    ) {
        this.directoryLookup = Objects.requireNonNull(directoryLookup);
        this.provisioningPort = Objects.requireNonNull(provisioningPort);
    }

    @Override
    public SecurityUserDetail provision(ProvisionLdapSecurityUserCommand command) {
        Objects.requireNonNull(command, "Provision LDAP security user command must not be null");

        String directoryUsername = requireNonBlank(
                command.directoryUsername(),
                "Directory username must not be blank"
        );
        String actorSubject = requireNonBlank(
                command.actorSubject(),
                "Actor subject must not be blank"
        );

        DirectoryUserProfile directoryUser =
                directoryLookup.lookup(new DirectoryUserLookupQuery(directoryUsername));

        if (directoryUser.accountStatus() != DirectoryAccountStatus.ACTIVE) {
            throw new LdapUserNotProvisionableException(directoryUser.accountStatus());
        }

        if (provisioningPort.ldapIdentityExists(
                directoryUser.trustDomain(),
                directoryUser.stableSubject()
        )) {
            throw new LdapProvisioningConflictException(
                    LdapProvisioningConflictException.Reason.ALREADY_PROVISIONED,
                    "LDAP identity is already provisioned"
            );
        }

        if (provisioningPort.usernameExists(directoryUser.username())) {
            throw new LdapProvisioningConflictException(
                    LdapProvisioningConflictException.Reason.USERNAME_CONFLICT,
                    "SIXPAY username is already in use"
            );
        }

        Set<String> roles = command.roles() == null ? Set.of() : Set.copyOf(command.roles());
        Set<String> permissions = command.permissions() == null ? Set.of() : Set.copyOf(command.permissions());

        return provisioningPort.createCanonicalLdapUser(
                UUID.randomUUID(),
                directoryUser,
                roles,
                permissions,
                actorSubject
        );
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
