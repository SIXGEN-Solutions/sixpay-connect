package com.sixpay.security.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SixpayRoleTest {

    @Test
    void shouldReturnRoleAuthority() {
        assertEquals(
                "ROLE_ADMIN",
                SixpayRole.ADMIN.authority()
        );

        assertEquals(
                "ROLE_OPS",
                SixpayRole.OPS.authority()
        );
    }
}