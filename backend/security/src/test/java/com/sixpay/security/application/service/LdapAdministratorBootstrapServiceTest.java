package com.sixpay.security.application.service;

import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.BootstrapLdapAdministratorCommand;
import com.sixpay.security.application.port.input.CreateSecurityUserCommand;
import com.sixpay.security.application.port.input.SecurityUserAdministrationUseCase;
import com.sixpay.security.application.port.output.SecurityUserAdministrationPort;
import com.sixpay.security.authorization.SixpayPermission;
import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LdapAdministratorBootstrapServiceTest {

    @Test
    void bootstrapsCanonicalAdminWithoutLocalCredentialAndLinksLdapIdentity() {
        SecurityUserAdministrationPort port = mock(SecurityUserAdministrationPort.class);
        SecurityUserAdministrationUseCase administration = mock(SecurityUserAdministrationUseCase.class);
        UUID userId = UUID.randomUUID();
        when(port.countUsers()).thenReturn(0L);

        SecurityUserDetail linked = new SecurityUserDetail(
                userId, "sixpay.testuser", "admin@sixpay.test",
                SixpayUserAccountStatus.ACTIVE, false, false,
                Set.of("ADMIN"), SixpayPermission.valuesAsSet(),
                List.of(), List.of()
        );
        when(administration.linkLdapIdentity(
                userId, "sixpay-preproduction-ldap",
                "0d308194-5949-4b3c-8946-28523a3d7828", "bootstrap"
        )).thenReturn(linked);

        var service = new LdapAdministratorBootstrapService(port, administration);
        SecurityUserDetail result = service.bootstrap(
                new BootstrapLdapAdministratorCommand(
                        userId, "sixpay.testuser", "admin@sixpay.test",
                        "sixpay-preproduction-ldap",
                        "0d308194-5949-4b3c-8946-28523a3d7828",
                        "bootstrap"
                )
        );

        ArgumentCaptor<CreateSecurityUserCommand> create =
                ArgumentCaptor.forClass(CreateSecurityUserCommand.class);
        verify(administration).createUser(create.capture());
        assertThat(create.getValue().roles()).containsExactly("ADMIN");
        assertThat(create.getValue().permissions())
                .containsExactlyInAnyOrderElementsOf(SixpayPermission.valuesAsSet());
        assertThat(create.getValue().localAuthenticationEnabled()).isFalse();
        assertThat(create.getValue().initialPassword()).isNull();
        verify(administration).linkLdapIdentity(
                userId, "sixpay-preproduction-ldap",
                "0d308194-5949-4b3c-8946-28523a3d7828", "bootstrap"
        );
        assertThat(result).isSameAs(linked);
    }

    @Test
    void failsClosedWhenAnyCanonicalUserAlreadyExists() {
        SecurityUserAdministrationPort port = mock(SecurityUserAdministrationPort.class);
        SecurityUserAdministrationUseCase administration = mock(SecurityUserAdministrationUseCase.class);
        when(port.countUsers()).thenReturn(1L);

        var service = new LdapAdministratorBootstrapService(port, administration);

        assertThatThrownBy(() -> service.bootstrap(
                new BootstrapLdapAdministratorCommand(
                        UUID.randomUUID(), "sixpay.testuser", null,
                        "sixpay-preproduction-ldap", "subject", "bootstrap"
                )
        )).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("empty SIXPAY user store");

        verifyNoInteractions(administration);
    }

    @Test
    void rejectsIncompletePrivilegedIdentityBeforeCreation() {
        SecurityUserAdministrationPort port = mock(SecurityUserAdministrationPort.class);
        SecurityUserAdministrationUseCase administration = mock(SecurityUserAdministrationUseCase.class);
        when(port.countUsers()).thenReturn(0L);

        var service = new LdapAdministratorBootstrapService(port, administration);

        assertThatThrownBy(() -> service.bootstrap(
                new BootstrapLdapAdministratorCommand(
                        UUID.randomUUID(), "sixpay.testuser", null,
                        " ", "subject", "bootstrap"
                )
        )).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(administration);
    }
}
