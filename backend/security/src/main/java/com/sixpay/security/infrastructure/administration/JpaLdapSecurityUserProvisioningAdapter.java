package com.sixpay.security.infrastructure.administration;

import com.sixpay.security.application.port.input.LdapProvisioningConflictException;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.CreateSecurityUserCommand;
import com.sixpay.security.application.port.input.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.output.LdapSecurityUserProvisioningPort;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserIdentitySpringDataRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class JpaLdapSecurityUserProvisioningAdapter
        implements LdapSecurityUserProvisioningPort {

    private final SecurityUserAccountSpringDataRepository userRepository;
    private final SecurityUserIdentitySpringDataRepository identityRepository;
    private final SecurityUserAdministrationUseCase administrationUseCase;

    public JpaLdapSecurityUserProvisioningAdapter(
            SecurityUserAccountSpringDataRepository userRepository,
            SecurityUserIdentitySpringDataRepository identityRepository,
            SecurityUserAdministrationUseCase administrationUseCase
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.identityRepository = Objects.requireNonNull(identityRepository);
        this.administrationUseCase = Objects.requireNonNull(administrationUseCase);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ldapIdentityExists(
            String trustDomain,
            String stableSubject
    ) {
        return identityRepository.existsByIdentityTypeAndProviderAndProviderSubject(
                AuthenticationIdentityType.LDAP,
                trustDomain,
                stableSubject
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByNormalizedUsername(
                username.trim().toLowerCase(Locale.ROOT)
        );
    }

    @Override
    @Transactional
    public SecurityUserDetail createCanonicalLdapUser(
            UUID userId,
            DirectoryUserProfile directoryUser,
            Set<String> roles,
            Set<String> permissions,
            String actorSubject
    ) {
        try {
            SecurityUserDetail created =
                    administrationUseCase.createUser(
                            new CreateSecurityUserCommand(
                                    userId,
                                    directoryUser.username(),
                                    directoryUser.email(),
                                    roles,
                                    permissions,
                                    false,
                                    null,
                                    actorSubject
                            )
                    );

            return administrationUseCase.linkLdapIdentity(
                    created.id(),
                    directoryUser.trustDomain(),
                    directoryUser.stableSubject(),
                    actorSubject
            );
        } catch (DataIntegrityViolationException exception) {
            if (identityRepository.existsByIdentityTypeAndProviderAndProviderSubject(
                    AuthenticationIdentityType.LDAP,
                    directoryUser.trustDomain(),
                    directoryUser.stableSubject()
            )) {
                throw new LdapProvisioningConflictException(
                        LdapProvisioningConflictException.Reason.ALREADY_PROVISIONED,
                        "LDAP identity is already provisioned"
                );
            }

            if (userRepository.existsByNormalizedUsername(
                    directoryUser.username().trim().toLowerCase(Locale.ROOT)
            )) {
                throw new LdapProvisioningConflictException(
                        LdapProvisioningConflictException.Reason.USERNAME_CONFLICT,
                        "SIXPAY username is already in use"
                );
            }

            throw exception;
        }
    }
}
