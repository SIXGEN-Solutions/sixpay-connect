package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.input.AuthenticateLdapIdentityUseCase;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import com.sixpay.security.domain.authentication.LdapAuthenticationResult;

import java.util.Objects;

public final class ActiveDirectoryLdapAuthenticationAdapter
        implements AuthenticateLdapIdentityUseCase {

    private final AuthenticationCapabilitiesProperties.Ldap properties;
    private final ActiveDirectoryDirectoryUserLookupAdapter directoryLookup;

    public ActiveDirectoryLdapAuthenticationAdapter(
            AuthenticationCapabilitiesProperties.Ldap properties,
            ActiveDirectoryDirectoryUserLookupAdapter directoryLookup
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "LDAP properties must not be null"
        );
        this.directoryLookup = Objects.requireNonNull(
                directoryLookup,
                "Directory lookup adapter must not be null"
        );
    }

    @Override
    public LdapAuthenticationResult authenticate(
            LdapAuthenticationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "LDAP authentication command must not be null"
        );

        try {
            DirectoryUserProfile profile =
                    directoryLookup.lookupByUsername(
                            command.username()
                    );

            requireAuthenticatable(profile.accountStatus());

            long deadlineNanos =
                    directoryLookup.deadlineNanos(
                            properties.authenticationTimeout().toNanos()
                    );

            directoryLookup.contextSource(
                    profile.username(),
                    command.password(),
                    deadlineNanos
            ).getReadOnlyContext().close();

            directoryLookup.requireWithinBudget(deadlineNanos);

            ExternalIdentity externalIdentity =
                    new ExternalIdentity(
                            profile.trustDomain(),
                            profile.stableSubject(),
                            profile.username()
                    );

            return new LdapAuthenticationResult(
                    AuthenticationIdentityType.LDAP,
                    externalIdentity
            );
        } catch (LdapAuthenticationFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LdapAuthenticationFailedException(exception);
        }
    }

    private static void requireAuthenticatable(
            DirectoryAccountStatus status
    ) {
        if (status != DirectoryAccountStatus.ACTIVE) {
            throw new LdapAuthenticationFailedException();
        }
    }
}
