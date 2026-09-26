package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import org.junit.jupiter.api.Test;

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
}
