package com.sixpay.security.application.service;

import com.sixpay.security.application.port.input.DirectoryUnavailableException;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserCommand;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisabledLdapServicesTest {

    @Test
    void directoryLookupFailsClosedWithoutLdap() {
        var service = new DisabledDirectoryUserLookupService();
        assertThatThrownBy(() -> service.lookup(new DirectoryUserLookupQuery("jane.doe")))
                .isInstanceOf(DirectoryUnavailableException.class);
    }

    @Test
    void provisioningFailsClosedWithoutLdap() {
        var service = new DisabledLdapSecurityUserProvisioningService();
        assertThatThrownBy(() -> service.provision(
                new ProvisionLdapSecurityUserCommand(
                        "jane.doe", Set.of("ADMIN"), Set.of(), "admin-subject")))
                .isInstanceOf(DirectoryUnavailableException.class);
    }
}
