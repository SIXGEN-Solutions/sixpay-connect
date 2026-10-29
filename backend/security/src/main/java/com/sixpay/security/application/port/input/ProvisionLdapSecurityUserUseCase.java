package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.SecurityUserDetail;

@FunctionalInterface
public interface ProvisionLdapSecurityUserUseCase {
    SecurityUserDetail provision(ProvisionLdapSecurityUserCommand command);
}
