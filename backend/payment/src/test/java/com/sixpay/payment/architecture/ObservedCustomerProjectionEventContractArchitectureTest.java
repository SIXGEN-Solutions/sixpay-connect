package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerProjectionEventContractArchitectureTest {
    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/payment/application/event/projection"
    );

    @Test
    void contractIsPaymentOwnedFrameworkFreeAndVersioned()
            throws Exception {
        for (String file : List.of(
                "ObservedCustomerProjectionEvent.java",
                "ObservedCustomerProjectionEventType.java",
                "ObservedCustomerProjectionPayload.java",
                "ProjectionPaymentStatus.java",
                "package-info.java"
        )) {
            Path path = ROOT.resolve(file);
            assertTrue(Files.isRegularFile(path));
            String source = Files.readString(path);
            for (String forbidden : List.of(
                    "import com.sixpay.customer.",
                    "import com.sixpay.payment.infrastructure.",
                    "import org.springframework.",
                    "import jakarta.",
                    "Amplitude",
                    "RestClient",
                    "WebClient",
                    "HttpClient",
                    "accountNumber",
                    "ribDebiteur",
                    "IntegrationAccountToken",
                    "DebtorAccountReference"
            )) {
                assertFalse(
                        source.contains(forbidden),
                        () -> file + " contains " + forbidden
                );
            }
        }
        String event = Files.readString(
                ROOT.resolve("ObservedCustomerProjectionEvent.java")
        );
        assertTrue(event.contains("CURRENT_EVENT_VERSION = 1"));
        assertTrue(event.contains("UUID eventId"));
        assertTrue(event.contains("long aggregateVersion"));
        assertTrue(event.contains("String correlationId"));
        assertTrue(event.contains("Instant occurredAt"));
    }
}
