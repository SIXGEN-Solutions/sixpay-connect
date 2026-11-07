package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActiveDirectoryLdapAuthenticationAdapterTest {

    @Test
    void rejectsDisabledDirectoryAccountBeforeUserBind() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FixedProfileLookupAdapter(
                        profile(DirectoryAccountStatus.DISABLED)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void rejectsLockedDirectoryAccountBeforeUserBind() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FixedProfileLookupAdapter(
                        profile(DirectoryAccountStatus.LOCKED)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void rejectsPasswordExpiredDirectoryAccountBeforeUserBind() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FixedProfileLookupAdapter(
                        profile(DirectoryAccountStatus.PASSWORD_EXPIRED)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void rejectsPasswordChangeRequiredDirectoryAccountBeforeUserBind() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FixedProfileLookupAdapter(
                        profile(DirectoryAccountStatus.PASSWORD_CHANGE_REQUIRED)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void rejectsExpiredDirectoryAccountBeforeUserBind() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FixedProfileLookupAdapter(
                        profile(DirectoryAccountStatus.EXPIRED)
                );

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void propagatesDirectoryLookupFailureAsAuthenticationFailure() {
        ActiveDirectoryDirectoryUserLookupAdapter lookup =
                new FailingLookupAdapter();

        ActiveDirectoryLdapAuthenticationAdapter adapter =
                new ActiveDirectoryLdapAuthenticationAdapter(
                        ldapProperties(),
                        lookup
                );

        assertThatThrownBy(
                () -> adapter.authenticate(
                        new LdapAuthenticationCommand(
                                "jane.doe",
                                "secret-password"
                        )
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
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

    private static AuthenticationCapabilitiesProperties.Ldap ldapProperties() {
        return new AuthenticationCapabilitiesProperties.Ldap(
                true,
                List.of("ldaps://directory.example.test"),
                "dc=example,dc=test",
                "ou=users",
                "(sAMAccountName={0})",
                "sAMAccountName",
                "sAMAccountName",
                "objectGUID",
                "regionale-ldap",
                "cn=service,dc=example,dc=test",
                "secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10)
        );
    }

    private static final class FixedProfileLookupAdapter
            extends ActiveDirectoryDirectoryUserLookupAdapter {

        private final DirectoryUserProfile profile;

        private FixedProfileLookupAdapter(
                DirectoryUserProfile profile
        ) {
            super(ldapProperties());
            this.profile = profile;
        }

        @Override
        public DirectoryUserProfile lookupByUsername(
                String username
        ) {
            return profile;
        }
    }

    private static final class FailingLookupAdapter
            extends ActiveDirectoryDirectoryUserLookupAdapter {

        private FailingLookupAdapter() {
            super(ldapProperties());
        }

        @Override
        public DirectoryUserProfile lookupByUsername(
                String username
        ) {
            throw new LdapAuthenticationFailedException();
        }
    }
}
