package com.sixpay.bootstrap.security;

import com.sixpay.security.application.port.input.BootstrapLdapAdministratorCommand;
import com.sixpay.security.application.port.input.BootstrapLdapAdministratorUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PreproductionLdapAdministratorBootstrapTest {

    @Test
    void delegatesOnlyThroughSecurityApplicationPort() {
        BootstrapLdapAdministratorUseCase useCase =
                mock(BootstrapLdapAdministratorUseCase.class);
        UUID userId = UUID.randomUUID();

        var runner = new PreproductionLdapAdministratorBootstrap(
                useCase, userId, "sixpay.testuser", "admin@sixpay.test",
                "sixpay-preproduction-ldap",
                "0d308194-5949-4b3c-8946-28523a3d7828"
        );
        runner.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<BootstrapLdapAdministratorCommand> command =
                ArgumentCaptor.forClass(BootstrapLdapAdministratorCommand.class);
        verify(useCase).bootstrap(command.capture());

        assertThat(command.getValue().userId()).isEqualTo(userId);
        assertThat(command.getValue().username()).isEqualTo("sixpay.testuser");
        assertThat(command.getValue().trustDomain())
                .isEqualTo("sixpay-preproduction-ldap");
        assertThat(command.getValue().stableSubject())
                .isEqualTo("0d308194-5949-4b3c-8946-28523a3d7828");
        assertThat(command.getValue().actorSubject())
                .isEqualTo(PreproductionLdapAdministratorBootstrap.ACTOR);
    }
}
