package com.sixpay.notification.infrastructure.operational.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationEmailMaskerTest {

    @Test
    void masksLocalPartButKeepsOperationalDomain() {
        assertEquals(
                "o***s@sixpay.example",
                NotificationEmailMasker.mask(
                        "operations@sixpay.example"
                )
        );
    }

    @Test
    void invalidAddressIsFullyMasked() {
        assertEquals(
                "***",
                NotificationEmailMasker.mask(
                        "invalid"
                )
        );
    }
}
