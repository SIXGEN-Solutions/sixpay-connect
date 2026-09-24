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
                        "api/partner/PartnerPaymentCommandController.java"
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
    void callbackUsesHmacSignatureAndCorrelationHeader()
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
                        "infrastructure/callback/partner/"
                                + "PartnerPaymentCallbackPayload.java"
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
                "partnerPaymentReference"
        ));
        assertTrue(payload.contains(
                "String externalPaymentReference"
        ));
        assertFalse(payload.contains(
                "partnerPaymentReference"
        ));
        assertTrue(payload.contains(
                "message.externalPaymentReference()"
        ));
        assertTrue(transport.contains(
                "PartnerPaymentCallbackPayload.from(delivery.message())"
        ));
    }
}
