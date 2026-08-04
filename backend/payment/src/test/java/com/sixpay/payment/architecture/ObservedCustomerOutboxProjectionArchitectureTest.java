package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerOutboxProjectionArchitectureTest {

    private static final Path PAYMENT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void paymentProjectionUsesCanonicalDomainEventIdentity()
            throws Exception {

        String service = normalizedSource(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentObservedCustomerProjectionService.java"
                )
        );

        String factory = normalizedSource(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentObservedCustomerProjectionRequestFactory.java"
                )
        );

        assertTrue(
                service.contains("PaymentDomainEventevent"),
                "Projection service must receive a PaymentDomainEvent"
        );

        assertTrue(
                service.contains(
                        "paymentLookupPort.findById(event.paymentId())"
                ),
                "Projection service must reload Payment using "
                        + "the event paymentId"
        );

        assertTrue(
                factory.contains("event.eventId()"),
                "Payment eventId must become the projection sourceEventId"
        );

        assertTrue(
                factory.contains("event.occurredAt()"),
                "Payment event occurredAt must become observedAt"
        );

        assertTrue(
                factory.contains(
                        "event.correlationId().value()"
                ),
                "The original Payment correlation ID must be propagated"
        );
    }

    @Test
    void paymentProjectionHasNoCustomerOrDeliveryDependency()
            throws Exception {

        for (String relative : List.of(
                "application/port/output/"
                        + "ObservedCustomerProjectionPort.java",
                "application/port/output/"
                        + "ObservedCustomerProjectionRequest.java",
                "application/port/output/"
                        + "ObservedCustomerProjectionResult.java",
                "application/service/"
                        + "PaymentObservedCustomerProjectionService.java",
                "application/service/"
                        + "PaymentObservedCustomerProjectionRequestFactory.java"
        )) {
            String source = Files.readString(
                    PAYMENT_ROOT.resolve(relative)
            );

            for (String forbidden : List.of(
                    "import com.sixpay.customer.",
                    "import org.springframework.",
                    "RestClient",
                    "WebClient",
                    "KafkaTemplate",
                    "@KafkaListener",
                    "@Scheduled",
                    "PaymentOutboxEntity"
            )) {
                assertFalse(
                        source.contains(forbidden),
                        () -> relative
                                + " contains forbidden dependency: "
                                + forbidden
                );
            }
        }
    }

    private static String normalizedSource(
            Path sourcePath
    ) throws Exception {
        return Files.readString(sourcePath)
                .replaceAll("\\s+", "");
    }
}