package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import org.junit.jupiter.api.Test;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActiveDirectoryLdapAuthenticationAdapterTest {

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
                ActiveDirectoryLdapAuthenticationAdapter.objectGuid(
                        adBytes
                )
        ).isEqualTo(expected.toString());
    }

    @Test
    void rejectsInvalidObjectGuidLength() {
        assertThatThrownBy(
                        () ->
                                ActiveDirectoryLdapAuthenticationAdapter
                                        .objectGuid(
                                                new byte[] {1, 2, 3}
                                        )
                )
                .isInstanceOf(
                        LdapAuthenticationFailedException.class
                );
    }

    @Test
    void detectsDisabledActiveDirectoryAccount() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("userAccountControl", "514"));

        ActiveDirectoryLdapAuthenticationAdapter.DirectoryAccountState state =
                ActiveDirectoryLdapAuthenticationAdapter.accountState(
                        attributes
                );

        assertThat(state.disabled()).isTrue();
    }

    @Test
    void detectsLockedAndPasswordExpiredComputedState() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "msDS-User-Account-Control-Computed",
                        String.valueOf(0x0010 | 0x800000)
                )
        );

        ActiveDirectoryLdapAuthenticationAdapter.DirectoryAccountState state =
                ActiveDirectoryLdapAuthenticationAdapter.accountState(
                        attributes
                );

        assertThat(state.locked()).isTrue();
        assertThat(state.passwordExpired()).isTrue();
    }

    @Test
    void detectsPasswordMustChangeFromPwdLastSet() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("pwdLastSet", "0"));

        ActiveDirectoryLdapAuthenticationAdapter.DirectoryAccountState state =
                ActiveDirectoryLdapAuthenticationAdapter.accountState(
                        attributes
                );

        assertThat(state.passwordMustChange()).isTrue();
    }

    @Test
    void detectsExpiredActiveDirectoryAccount() throws Exception {
        Instant expiration = Instant.parse("2026-09-25T00:00:00Z");
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

        ActiveDirectoryLdapAuthenticationAdapter.DirectoryAccountState state =
                ActiveDirectoryLdapAuthenticationAdapter.accountState(
                        attributes
                );

        assertThat(
                state.accountExpired(
                        Instant.parse("2026-09-26T00:00:00Z")
                )
        ).isTrue();
    }

    @Test
    void treatsZeroAccountExpiresAsNoExpiration() throws Exception {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("accountExpires", "0"));

        ActiveDirectoryLdapAuthenticationAdapter.DirectoryAccountState state =
                ActiveDirectoryLdapAuthenticationAdapter.accountState(
                        attributes
                );

        assertThat(
                state.accountExpired(
                        Instant.parse("2030-01-01T00:00:00Z")
                )
        ).isFalse();
    }

    @Test
    void failsClosedOnMalformedActiveDirectoryState() {
        BasicAttributes attributes = new BasicAttributes();
        attributes.put(
                new BasicAttribute(
                        "userAccountControl",
                        "not-a-number"
                )
        );

        assertThatThrownBy(
                        () ->
                                ActiveDirectoryLdapAuthenticationAdapter
                                        .accountState(attributes)
                )
                .isInstanceOf(
                        LdapAuthenticationFailedException.class
                );
    }
}
