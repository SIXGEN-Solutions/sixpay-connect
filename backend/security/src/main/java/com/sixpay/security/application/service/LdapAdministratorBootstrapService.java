package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.BootstrapLdapAdministratorCommand;
import com.sixpay.security.application.port.input.BootstrapLdapAdministratorUseCase;
import com.sixpay.security.application.port.input.CreateSecurityUserCommand;
import com.sixpay.security.application.port.input.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.output.SecurityUserAdministrationPort;
import com.sixpay.security.authorization.SixpayPermission;
import com.sixpay.security.authorization.SixpayRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * One-shot privileged bootstrap for a brand-new SIXPAY security realm.
 *
 * <p>Security owns this capability. Runtime composition may trigger it,
 * but no other module reaches Security persistence directly.</p>
 */
public class LdapAdministratorBootstrapService
        implements BootstrapLdapAdministratorUseCase {

    private final SecurityUserAdministrationPort administrationPort;
    private final SecurityUserAdministrationUseCase administrationUseCase;

    public LdapAdministratorBootstrapService(
            SecurityUserAdministrationPort administrationPort,
            SecurityUserAdministrationUseCase administrationUseCase
    ) {
        this.administrationPort = Objects.requireNonNull(administrationPort);
        this.administrationUseCase = Objects.requireNonNull(administrationUseCase);
    }

    @Override
    @Transactional
    public SecurityUserDetail bootstrap(BootstrapLdapAdministratorCommand command) {
        Objects.requireNonNull(
                command,
                "LDAP administrator bootstrap command must not be null"
        );

        if (administrationPort.countUsers() != 0L) {
            throw new IllegalStateException(
                    "LDAP administrator bootstrap requires an empty SIXPAY user store; "
                            + "disable bootstrap after initial provisioning"
            );
        }

        UUID userId = Objects.requireNonNull(
                command.userId(),
                "Bootstrap administrator user id must not be null"
        );
        String username = requireText(
                command.username(),
                "Bootstrap administrator username must not be blank"
        );
        String trustDomain = requireText(
                command.trustDomain(),
                "Bootstrap LDAP trust domain must not be blank"
        );
        String stableSubject = requireText(
                command.stableSubject(),
                "Bootstrap LDAP stable subject must not be blank"
        );
        String actor = requireText(
                command.actorSubject(),
                "Bootstrap actor subject must not be blank"
        );

        administrationUseCase.createUser(
                new CreateSecurityUserCommand(
                        userId,
                        username,
                        normalizeOptional(command.email()),
                        Set.of(SixpayRole.ADMIN.name()),
                        SixpayPermission.valuesAsSet(),
                        false,
                        null,
                        actor
                )
        );

        return administrationUseCase.linkLdapIdentity(
                userId,
                trustDomain,
                stableSubject,
                actor
        );
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
