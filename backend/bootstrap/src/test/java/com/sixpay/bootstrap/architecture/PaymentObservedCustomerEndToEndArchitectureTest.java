package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentObservedCustomerEndToEndArchitectureTest {

    private static final Path PAYMENT_ROOT =
            Path.of("../payment/src/main/java/com/sixpay/payment");

    private static final Path CUSTOMER_ROOT =
            Path.of("../customer/src/main/java/com/sixpay/customer");

    private static final Path BOOTSTRAP_ROOT =
            Path.of("src/main/java/com/sixpay/bootstrap");

    @Test
    void customerNeverDependsOnPayment()
            throws IOException {

        assertNoToken(
                CUSTOMER_ROOT,
                "import com.sixpay.payment."
        );
    }

    @Test
    void paymentApplicationNeverDependsOnCustomer()
            throws IOException {

        assertNoToken(
                PAYMENT_ROOT.resolve("application"),
                "import com.sixpay.customer."
        );
    }

    @Test
    void schedulerIsAbsentFromDomainAndApplicationLayers()
            throws IOException {

        for (Path root : List.of(
                PAYMENT_ROOT.resolve("domain"),
                PAYMENT_ROOT.resolve("application"),
                CUSTOMER_ROOT.resolve("verification/domain"),
                CUSTOMER_ROOT.resolve("verification/application"),
                CUSTOMER_ROOT.resolve("observation/domain"),
                CUSTOMER_ROOT.resolve("observation/application")
        )) {
            assertNoToken(root, "@Scheduled");
            assertNoToken(
                    root,
                    "org.springframework.scheduling"
            );
        }
    }

    @Test
    void paymentPortDoesNotExposeJpaOutboxOrCustomerTypes()
            throws IOException {

        Path portRoot =
                PAYMENT_ROOT.resolve(
                        "application/port/output"
                );

        for (String forbidden : List.of(
                "PaymentOutboxEntity",
                "jakarta.persistence",
                "org.hibernate",
                "import com.sixpay.customer.",
                "ObserveCustomerCommand",
                "ObserveCustomerResult"
        )) {
            assertNoToken(portRoot, forbidden);
        }
    }

    @Test
    void durableProjectionContractContainsNoAmplitudeDtoOrRawAccount()
            throws IOException {

        Path contractRoot =
                PAYMENT_ROOT.resolve(
                        "application/event/projection"
                );

        for (String forbidden : List.of(
                "import com.sixpay.customer.",
                "import com.sixpay.payment.infrastructure.",
                "AmplitudeCustomerVerification",
                "AmplitudeErrorResponse",
                "RestClient",
                "WebClient",
                "HttpClient",
                "accountNumber",
                "ribDebiteur",
                "IntegrationAccountToken",
                "DebtorAccountReference"
        )) {
            assertNoToken(contractRoot, forbidden);
        }
    }

    @Test
    void projectionNeverReloadsCurrentPaymentState()
            throws IOException {

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
                "paymentLookupPort",
                "findById(",
                "PaymentDomainEvent",
                "Payment payment",
                "payment.toState()",
                "from(payment, event)"
        )) {
            assertFalse(
                    service.contains(forbidden)
                            || factory.contains(forbidden),
                    () -> "Current Payment state dependency: "
                            + forbidden
            );
        }

        assertTrue(
                service.contains(
                        "ObservedCustomerProjectionEvent"
                )
        );

        assertTrue(
                service.contains(
                        "requestFactory.from(event)"
                )
        );
    }

    @Test
    void bootstrapIsTheOnlyIntermoduleCompositionPoint()
            throws IOException {

        assertNoToken(
                PAYMENT_ROOT,
                "import com.sixpay.customer."
        );

        assertNoToken(
                CUSTOMER_ROOT,
                "import com.sixpay.payment."
        );

        assertTrue(
                sourcesContain(
                        BOOTSTRAP_ROOT.resolve(
                                "integration/customer"
                        ),
                        "import com.sixpay.payment."
                )
        );

        assertTrue(
                sourcesContain(
                        BOOTSTRAP_ROOT.resolve(
                                "integration/customer"
                        ),
                        "import com.sixpay.customer."
                )
        );
    }

    @Test
    void dispatcherAcknowledgesOnlyAfterHandlerSuccess()
            throws IOException {

        String dispatcher = Files.readString(
                BOOTSTRAP_ROOT.resolve(
                        "integration/customer/outbox/"
                                + "PaymentObservedCustomerOutboxDispatcher.java"
                )
        );

        int handler = dispatcher.indexOf(
                "handler.handle(event)"
        );

        int published = dispatcher.indexOf(
                "completionService.markPublished("
        );

        assertTrue(handler >= 0);
        assertTrue(published > handler);
    }

    private static void assertNoToken(
            Path root,
            String token
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> contains(path, token))
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> token + " found in " + violations
            );
        }
    }

    private static boolean sourcesContain(
            Path root,
            String token
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .anyMatch(path -> contains(path, token));
        }
    }

    private static boolean contains(
            Path path,
            String token
    ) {
        try {
            return Files.readString(path).contains(token);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot inspect " + path,
                    exception
            );
        }
    }
}
