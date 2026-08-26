package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAsyncCallbackArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void callbackIsDrivenByOutboxAndNotHttpTransaction()
            throws Exception {
        String controller = Files.readString(
                ROOT.resolve(
                        "api/PaymentCommandController.java"
                )
        );
        String relay = Files.readString(
                ROOT.resolve(
                        "infrastructure/callback/relay/"
                                + "PaymentCallbackOutboxRelay.java"
                )
        );

        assertFalse(controller.contains(
                "PaymentStatusCallbackTransportPort"
        ));
        assertFalse(controller.contains("callbackUrl"));
        assertTrue(relay.contains("@Scheduled"));
        assertTrue(relay.contains(
                "PaymentStatusCallbackTransportPort"
        ));
    }

    @Test
    void callbackUsesDetachedJwsAndCorrelationHeader()
            throws Exception {
        String transport = Files.readString(
                ROOT.resolve(
                        "infrastructure/callback/"
                                + "PaymentStatusCallbackHttpAdapter.java"
                )
        );

        assertTrue(transport.contains(
                "X-SIXPAY-Signature"
        ));
        assertTrue(transport.contains(
                "X-Correlation-ID"
        ));
    }
}
