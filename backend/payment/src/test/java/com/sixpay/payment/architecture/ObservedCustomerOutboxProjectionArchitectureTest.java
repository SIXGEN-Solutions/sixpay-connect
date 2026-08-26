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
    void paymentProjectionUsesDurableEventSnapshot()
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
                service.contains(
                        "ObservedCustomerProjectionEventevent"
                ),
                "Projection service must receive the durable "
                        + "ObservedCustomerProjectionEvent"
        );

        assertTrue(
                service.contains(
                        "requestFactory.from(event)"
                ),
                "Projection request must be created directly "
                        + "from the durable event"
        );

        assertTrue(
                service.contains(
                        "projectionPort.project("
                ),
                "Projection service must invoke the "
                        + "Payment-owned projection port"
        );

        assertFalse(
                service.contains("PaymentLookupPort"),
                "Projection service must not depend on "
                        + "PaymentLookupPort"
        );

        assertFalse(
                service.contains("paymentLookupPort"),
                "Projection service must not reload Payment"
        );

        assertFalse(
                service.contains("findById("),
                "Projection service must not reload the "
                        + "current aggregate state"
        );

        assertFalse(
                service.contains("PaymentDomainEvent"),
                "The dispatcher flow must consume the durable "
                        + "projection event, not the original "
                        + "domain event"
        );

        assertTrue(
                factory.contains("event.eventId()"),
                "Durable eventId must become the projection "
                        + "sourceEventId"
        );

        assertTrue(
                factory.contains("event.paymentId()"),
                "Durable paymentId must be propagated"
        );

        assertTrue(
                factory.contains("event.payload()"),
                "Projection data must come from the durable "
                        + "event payload"
        );

        assertTrue(
                factory.contains("event.occurredAt()"),
                "Durable occurredAt must become observedAt"
        );

        assertTrue(
                factory.contains("event.correlationId()"),
                "Original correlation ID must be propagated"
        );

        assertFalse(
                factory.contains("Paymentpayment"),
                "Request factory must no longer receive the "
                        + "current Payment aggregate"
        );

        assertFalse(
                factory.contains("payment.toState()"),
                "Request factory must not extract the current "
                        + "Payment state"
        );

        assertFalse(
                factory.contains(
                        "requestFactory.from(payment,event)"
                ),
                "Request factory must not combine current state "
                        + "with a historical event"
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

    @Test
    void durableProjectionDoesNotReloadCurrentPaymentState()
            throws Exception {

        String service = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentObservedCustomerProjectionService.java"
                )
        );

        String factory = Files.readString(
                PAYMENT_ROOT.resolve(
                        "application/service/"
                                + "PaymentObservedCustomerProjectionRequestFactory.java"
                )
        );

        for (String forbidden : List.of(
                "PaymentLookupPort",
                "Payment payment",
                "PaymentState",
                "payment.toState()",
                "findById(event.paymentId())",
                "from(payment, event)"
        )) {
            assertFalse(
                    service.contains(forbidden)
                            || factory.contains(forbidden),
                    () -> "Durable projection reloads current "
                            + "Payment state through: "
                            + forbidden
            );
        }
    }

    private static String normalizedSource(
            Path sourcePath
    ) throws Exception {
        return Files.readString(sourcePath)
                .replaceAll("\\s+", "");
    }
}