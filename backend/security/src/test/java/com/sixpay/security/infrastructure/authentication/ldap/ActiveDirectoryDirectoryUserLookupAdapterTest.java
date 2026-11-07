package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import org.junit.jupiter.api.Test;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActiveDirectoryDirectoryUserLookupAdapterTest {

    @Test
    void convertsActiveDirectoryObjectGuidToCanonicalUuid() {
        UUID expected =
                UUID.fromString(
                        "00112233-4455-6677-8899-aabbccddeeff"
                );

        byte[] adBytes = new byte[] {
                0x33, 0x22, 0x11, 0x00,
                0x55, 0x44,
                0x77, 0x66,
                (byte) 0x88, (byte) 0x99,
                (byte) 0xaa, (byte) 0xbb,
                (byte) 0xcc, (byte) 0xdd,
                (byte) 0xee, (byte) 0xff
        };

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.objectGuid(
                        adBytes
                )
        ).isEqualTo(expected.toString());
    }

    @Test
    void rejectsInvalidObjectGuidLength() {
        assertThatThrownBy(
                () -> ActiveDirectoryDirectoryUserLookupAdapter.objectGuid(
                        new byte[] {1, 2, 3}
                )
        ).isInstanceOf(LdapAuthenticationFailedException.class);
    }

    @Test
    void mapsDisabledAccount() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "userAccountControl",
                        "514"
                )
        );

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-10-04T00:00:00Z")
                )
        ).isEqualTo(DirectoryAccountStatus.DISABLED);
    }

    @Test
    void mapsLockedAccount() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "msDS-User-Account-Control-Computed",
                        String.valueOf(0x0010)
                )
        );

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-10-04T00:00:00Z")
                )
        ).isEqualTo(DirectoryAccountStatus.LOCKED);
    }

    @Test
    void mapsPasswordExpiredAccount() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "msDS-User-Account-Control-Computed",
                        String.valueOf(0x800000)
                )
        );

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-10-04T00:00:00Z")
                )
        ).isEqualTo(DirectoryAccountStatus.PASSWORD_EXPIRED);
    }

    @Test
    void mapsPasswordChangeRequired() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("pwdLastSet", "0"));

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-10-04T00:00:00Z")
                )
        ).isEqualTo(
                DirectoryAccountStatus.PASSWORD_CHANGE_REQUIRED
        );
    }

    @Test
    void mapsExpiredAccount() throws Exception {
        Instant expiration =
                Instant.parse("2026-09-25T00:00:00Z");

        long fileTime =
                (expiration.getEpochSecond() + 11_644_473_600L)
                        * 10_000_000L
                        + expiration.getNano() / 100L;

        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "accountExpires",
                        Long.toString(fileTime)
                )
        );

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-09-26T00:00:00Z")
                )
        ).isEqualTo(DirectoryAccountStatus.EXPIRED);
    }

    @Test
    void mapsActiveAccount() throws Exception {
        BasicAttributes attributes = new BasicAttributes();

        var state =
                ActiveDirectoryDirectoryUserLookupAdapter.accountState(
                        attributes
                );

        assertThat(
                ActiveDirectoryDirectoryUserLookupAdapter.accountStatus(
                        state,
                        Instant.parse("2026-10-04T00:00:00Z")
                )
        ).isEqualTo(DirectoryAccountStatus.ACTIVE);
    }

    @Test
    void escapesSearchFilterValue() {
        ActiveDirectoryDirectoryUserLookupAdapter adapter =
                new ActiveDirectoryDirectoryUserLookupAdapter(
                        ldapProperties()
                );

        String encoded =
                adapter.resolveSearchFilter(
                        "john*)(uid=*)"
                );

        assertThat(encoded)
                .doesNotContain("john*)(uid=*)")
                .contains("\\2a")
                .contains("\\29")
                .contains("\\28");
    }

    @Test
    void failsClosedWhenBudgetIsExhausted() {
        ActiveDirectoryDirectoryUserLookupAdapter adapter =
                new ActiveDirectoryDirectoryUserLookupAdapter(
                        ldapProperties(),
                        () -> 100L,
                        Instant::now
                );

        assertThatThrownBy(
                () -> adapter.requireWithinBudget(100L)
        ).isInstanceOf(
                LdapAuthenticationFailedException.class
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
}
