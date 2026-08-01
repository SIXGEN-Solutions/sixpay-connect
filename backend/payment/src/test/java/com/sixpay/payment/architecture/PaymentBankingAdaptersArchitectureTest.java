package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentBankingAdaptersArchitectureTest {

    private static final Path PORT_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "application/port/out/banking"
    );

    private static final Path ADAPTER_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude"
    );

    @Test
    void exposesOnlyRequestedGateways() throws IOException {
        Set<String> expected = Set.of(
                "BankingIdempotencyKey.java",
                "BankingRequestContext.java",
                "FundsGateway.java",
                "LookupGateway.java",
                "PostingGateway.java",
                "ReversalGateway.java",
                "VerificationGateway.java",
                "package-info.java"
        );

        try (Stream<Path> paths = Files.list(PORT_ROOT)) {
            List<String> actual = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertEquals(
                    expected.stream().sorted().toList(),
                    actual
            );
        }
    }

    @Test
    void reservationCapabilityIsNotGenerated()
            throws IOException {
        try (Stream<Path> paths = Files.walk(PORT_ROOT)) {
            String sources = paths
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .reduce("", String::concat);

            assertFalse(sources.contains("reserveFunds"));
            assertFalse(sources.contains("releaseFunds"));
            assertFalse(sources.contains("ReservationGateway"));
        }
    }

    @Test
    void adaptersRemainInactiveWithoutConcreteClient()
            throws IOException {
        try (Stream<Path> paths = Files.list(ADAPTER_ROOT)) {
            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .endsWith("Adapter.java"))
                    .filter(path -> {
                        try {
                            return !Files.readString(path).contains(
                                    "@ConditionalOnBean("
                                            + "AmplitudeBankingClient.class"
                                            + ")"
                            );
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(violations.isEmpty());
        }
    }

    @Test
    void noConcreteHttpClientIsInvented()
            throws IOException {
        List<String> forbidden = List.of(
                "RestClient",
                "WebClient",
                "HttpClient",
                "@ConfigurationProperties",
                "baseUrl"
        );

        try (Stream<Path> paths = Files.walk(ADAPTER_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertEquals(List.of(), violations);
        }
    }
}
