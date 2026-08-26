package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentObservedCustomerDurableProjectionArchitectureTest {

    private static final Path SERVICE_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/application/service"
    );

    @Test
    void projectionNeverReloadsCurrentPaymentState() throws Exception {
        String service = Files.readString(
                SERVICE_ROOT.resolve(
                        "PaymentObservedCustomerProjectionService.java"
                )
        );
        String factory = Files.readString(
                SERVICE_ROOT.resolve(
                        "PaymentObservedCustomerProjectionRequestFactory.java"
                )
        );

        assertTrue(service.contains(
                "ObservedCustomerProjectionEvent event"
        ));
        assertTrue(service.contains(
                "requestFactory.from(event)"
        ));
        assertFalse(service.contains("PaymentLookupPort"));
        assertFalse(service.contains("findById("));
        assertFalse(factory.contains("Payment payment"));
        assertFalse(factory.contains("PaymentDomainEvent"));
        assertTrue(factory.contains("event.payload()"));
    }
}
