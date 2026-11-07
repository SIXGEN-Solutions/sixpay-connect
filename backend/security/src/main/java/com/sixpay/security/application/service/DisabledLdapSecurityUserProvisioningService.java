package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.DirectoryUnavailableException;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserCommand;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserUseCase;

/** Fail-closed LDAP provisioning for deployments without LDAP. */
public final class DisabledLdapSecurityUserProvisioningService
        implements ProvisionLdapSecurityUserUseCase {
    @Override
    public SecurityUserDetail provision(ProvisionLdapSecurityUserCommand command) {
        throw new DirectoryUnavailableException(
                new IllegalStateException("LDAP provisioning capability is disabled")
        );
    }
}
