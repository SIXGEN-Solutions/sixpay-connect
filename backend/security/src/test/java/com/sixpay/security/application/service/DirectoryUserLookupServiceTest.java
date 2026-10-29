package com.sixpay.security.application.service;

import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.input.DirectoryUserLookupQuery;
import com.sixpay.security.application.port.output.DirectoryUserLookupPort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DirectoryUserLookupServiceTest {

    @Test
    void delegatesExactDirectoryLookupWithoutMutationConcerns() {
        DirectoryUserProfile expected = new DirectoryUserProfile(
                "jane.doe",
                "Jane Doe",
                "jane.doe@example.test",
                "regionale-ldap",
                "00112233-4455-6677-8899-aabbccddeeff",
                DirectoryAccountStatus.ACTIVE
        );

        DirectoryUserLookupPort port = username -> {
            assertThat(username).isEqualTo("jane.doe");
            return expected;
        };

        DirectoryUserLookupService service =
                new DirectoryUserLookupService(port);

        DirectoryUserProfile actual = service.lookup(
                new DirectoryUserLookupQuery(" jane.doe ")
        );

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void rejectsBlankDirectoryUsernameBeforeCallingPort() {
        assertThatThrownBy(
                () -> new DirectoryUserLookupQuery("   ")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicProfileContainsOnlyApplicationDirectoryData() {
        DirectoryUserProfile profile = new DirectoryUserProfile(
                "jane.doe",
                "Jane Doe",
                null,
                "regionale-ldap",
                "00112233-4455-6677-8899-aabbccddeeff",
                DirectoryAccountStatus.ACTIVE
        );

        assertThat(profile.username()).isEqualTo("jane.doe");
        assertThat(profile.displayName()).isEqualTo("Jane Doe");
        assertThat(profile.email()).isNull();
        assertThat(profile.trustDomain()).isEqualTo("regionale-ldap");
        assertThat(profile.stableSubject())
                .isEqualTo("00112233-4455-6677-8899-aabbccddeeff");
        assertThat(profile.accountStatus())
                .isEqualTo(DirectoryAccountStatus.ACTIVE);
    }
}
