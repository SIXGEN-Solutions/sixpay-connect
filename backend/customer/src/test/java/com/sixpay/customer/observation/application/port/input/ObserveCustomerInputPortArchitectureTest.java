package com.sixpay.customer.observation.application.port.input;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserveCustomerInputPortArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "application/port/input"
    );

    @Test
    void inputPortRemainsFrameworkFreeAndCustomerOwned()
            throws Exception {

        try (var paths = Files.walk(ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.",
                                            "import org.hibernate.",
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.",
                                            "import com.sixpay.customer.observation.infrastructure.",
                                            "import com.sixpay.customer.observation.configuration.",
                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "KafkaTemplate",
                                            "@Entity",
                                            "@Repository",
                                            "@Service",
                                            "@Component",
                                            "Instant.now(",
                                            "UUID.randomUUID("
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Observation input-port violations: "
                            + violations
            );
        }
    }

    @Test
    void commandDoesNotExposeRawAccountOrPaymentTypes()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "ObserveCustomerCommand.java"
                )
        );

        for (String forbidden : List.of(
                "import com.sixpay.payment.",
                "com.sixpay.payment.domain.model.PaymentState",
                "com.sixpay.payment.domain.model.PaymentStatus",
                "com.sixpay.payment.domain.model.PaymentFailure",
                "com.sixpay.payment.domain.model.DebtorAccountReference",
                "accountNumber",
                "ribDebiteur",
                "rawAccount",
                "IntegrationAccountToken"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden input concept: "
                            + forbidden
            );
        }

        for (String required : List.of(
                "UUID sourceEventId",
                "UUID paymentId",
                "String normalizedNiu",
                "String accountBindingFingerprint",
                "String maskedAccountReference",
                "ObservedPaymentStatus paymentStatus",
                "String correlationId"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing observation input concept: "
                            + required
            );
        }
    }
}