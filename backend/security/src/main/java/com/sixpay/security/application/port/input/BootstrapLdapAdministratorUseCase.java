package com.sixpay.security.application.port.input;

import com.sixpay.security.application.model.SecurityUserDetail;

public interface BootstrapLdapAdministratorUseCase {
    SecurityUserDetail bootstrap(BootstrapLdapAdministratorCommand command);
}
