package com.sixpay.security.domain.authentication;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SixpayUserAccountAuthorityMappingTest {

    @Test
    void mapsCanonicalRolesAndPermissionsToSpringAuthorities() {
        SixpayUserAccount account =
                new SixpayUserAccount(
                        UUID.randomUUID(),
                        "admin",
                        "admin@sixpay.local",
                        SixpayUserAccountStatus.ACTIVE,
                        Set.of("ADMIN"),
                        Set.of(
                                "payment.read",
                                "SCOPE_observed-customer.read"
                        )
                );

        assertThat(account.authorities())
                .containsExactlyInAnyOrder(
                        "ROLE_ADMIN",
                        "SCOPE_payment.read",
                        "SCOPE_observed-customer.read"
                )
                .doesNotContain(
                        "payment.read",
                        "observed-customer.read"
                );
    }
}
