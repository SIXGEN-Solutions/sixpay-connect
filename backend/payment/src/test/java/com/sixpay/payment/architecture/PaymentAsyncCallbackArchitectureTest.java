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
                        "api/partner/tresorpay/TresorPayPaymentCommandController.java"
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
    @Test
    void callbackInternalMessageKeepsProviderNeutralVocabulary()
            throws Exception {
        String message = Files.readString(
                ROOT.resolve(
                        "application/port/output/callback/"
                                + "PaymentStatusCallbackMessage.java"
                )
        );
        String payload = Files.readString(
                ROOT.resolve(
                        "infrastructure/callback/tresorpay/"
                                + "TresorPayPaymentCallbackPayload.java"
                )
        );
        String transport = Files.readString(
                ROOT.resolve(
                        "infrastructure/callback/"
                                + "PaymentStatusCallbackHttpAdapter.java"
                )
        );

        assertTrue(message.contains(
                "String externalPaymentReference"
        ));
        assertFalse(message.contains(
                "tresorPayPaymentReference"
        ));
        assertTrue(payload.contains(
                "@JsonProperty(\"tresorPayPaymentReference\")"
        ));
        assertTrue(payload.contains(
                "message.externalPaymentReference()"
        ));
        assertTrue(transport.contains(
                "TresorPayPaymentCallbackPayload.from(delivery.message())"
        ));
    }
}
