package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.LdapProvisioningConflictException;
import com.sixpay.security.application.exception.LdapUserNotProvisionableException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.model.SecurityUserDetail;
import com.sixpay.security.application.port.input.DirectoryUserLookupUseCase;
import com.sixpay.security.application.port.input.ProvisionLdapSecurityUserCommand;
import com.sixpay.security.application.port.output.LdapSecurityUserProvisioningPort;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProvisionLdapSecurityUserServiceTest {

    @Test
    void revalidatesDirectoryBeforeAnySixpayPersistence() {
        DirectoryUserLookupUseCase lookup =
                query -> profile(DirectoryAccountStatus.ACTIVE);

        LdapSecurityUserProvisioningPort port =
                mock(LdapSecurityUserProvisioningPort.class);

        when(port.createCanonicalLdapUser(
                any(), any(), anySet(), anySet(), anyString()
        )).thenReturn(mock(SecurityUserDetail.class));

        new ProvisionLdapSecurityUserService(lookup, port).provision(
                new ProvisionLdapSecurityUserCommand(
                        " jane.doe ",
                        Set.of("AUDITOR"),
                        Set.of("payment.audit.read"),
                        "admin"
                )
        );

        verify(port).ldapIdentityExists(
                "regionale-ldap",
                "00112233-4455-6677-8899-aabbccddeeff"
        );
        verify(port).usernameExists("jane.doe");
        verify(port).createCanonicalLdapUser(
                any(UUID.class),
                eq(profile(DirectoryAccountStatus.ACTIVE)),
                eq(Set.of("AUDITOR")),
                eq(Set.of("payment.audit.read")),
                eq("admin")
        );
    }

    @Test
    void refusesNonActiveDirectoryAccount() {
        DirectoryUserLookupUseCase lookup =
                query -> profile(DirectoryAccountStatus.LOCKED);

        LdapSecurityUserProvisioningPort port =
                mock(LdapSecurityUserProvisioningPort.class);

        assertThatThrownBy(
                () -> new ProvisionLdapSecurityUserService(lookup, port).provision(
                        new ProvisionLdapSecurityUserCommand(
                                "jane.doe",
                                Set.of(),
                                Set.of(),
                                "admin"
                        )
                )
        ).isInstanceOf(LdapUserNotProvisionableException.class);

        verifyNoInteractions(port);
    }

    @Test
    void distinguishesAlreadyProvisionedIdentity() {
        LdapSecurityUserProvisioningPort port =
                mock(LdapSecurityUserProvisioningPort.class);

        when(port.ldapIdentityExists(anyString(), anyString()))
                .thenReturn(true);

        assertThatThrownBy(
                () -> new ProvisionLdapSecurityUserService(
                        query -> profile(DirectoryAccountStatus.ACTIVE),
                        port
                ).provision(
                        new ProvisionLdapSecurityUserCommand(
                                "jane.doe",
                                Set.of(),
                                Set.of(),
                                "admin"
                        )
                )
        )
                .isInstanceOf(LdapProvisioningConflictException.class)
                .extracting("reason")
                .isEqualTo(
                        LdapProvisioningConflictException.Reason.ALREADY_PROVISIONED
                );
    }

    @Test
    void distinguishesUsernameConflictWithoutAutoLink() {
        LdapSecurityUserProvisioningPort port =
                mock(LdapSecurityUserProvisioningPort.class);

        when(port.ldapIdentityExists(anyString(), anyString()))
                .thenReturn(false);
        when(port.usernameExists("jane.doe"))
                .thenReturn(true);

        assertThatThrownBy(
                () -> new ProvisionLdapSecurityUserService(
                        query -> profile(DirectoryAccountStatus.ACTIVE),
                        port
                ).provision(
                        new ProvisionLdapSecurityUserCommand(
                                "jane.doe",
                                Set.of(),
                                Set.of(),
                                "admin"
                        )
                )
        )
                .isInstanceOf(LdapProvisioningConflictException.class)
                .extracting("reason")
                .isEqualTo(
                        LdapProvisioningConflictException.Reason.USERNAME_CONFLICT
                );

        verify(port, never()).createCanonicalLdapUser(
                any(), any(), anySet(), anySet(), anyString()
        );
    }

    private static DirectoryUserProfile profile(
            DirectoryAccountStatus status
    ) {
        return new DirectoryUserProfile(
                "jane.doe",
                "Jane Doe",
                "jane.doe@example.test",
                "regionale-ldap",
                "00112233-4455-6677-8899-aabbccddeeff",
                status
        );
    }
}
