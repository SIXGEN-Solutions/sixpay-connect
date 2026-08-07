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
                    + "application/port/output/banking"
    );

    private static final Path ADAPTER_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude"
    );

    private static final Path RESERVATION_ROOT =
            ADAPTER_ROOT.resolve("reservation");

    @Test
    void exposesOnlyApprovedBankingGateways()
            throws IOException {

        Set<String> expected = Set.of(
                "BankingIdempotencyKey.java",
                "BankingRequestContext.java",
                "FundsGateway.java",
                "FundsReservationGateway.java",
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
    void reservationCapabilityIsExplicitlyLimited()
            throws IOException {

        String sources = readAllJavaSources(PORT_ROOT);

        assertTrue(
                sources.contains(
                        "interface FundsReservationGateway"
                )
        );

        assertTrue(
                sources.contains(
                        "FundsReservationSnapshot reserve("
                )
        );

        for (String forbidden : List.of(
                "releaseFunds",
                "cancelReservation",
                "lookupReservation"
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Capability not approved in Lot 5.4.2: "
                            + forbidden
            );
        }
    }

    @Test
    void existingAmplitudeAdaptersRemainConditionalOnGenericClient()
            throws IOException {

        for (String adapterName : List.of(
                "AmplitudeVerificationAdapter.java",
                "AmplitudeFundsAdapter.java",
                "AmplitudePostingAdapter.java",
                "AmplitudeLookupAdapter.java",
                "AmplitudeReversalAdapter.java"
        )) {
            assertAdapterConditionalOn(
                    adapterName,
                    "AmplitudeBankingClient"
            );
        }
    }

    @Test
    void reservationAdapterRemainsConditionalOnReservationClient()
            throws IOException {

        Path adapter = RESERVATION_ROOT.resolve(
                "AmplitudeFundsReservationAdapter.java"
        );

        assertTrue(
                Files.isRegularFile(adapter),
                "Missing reservation adapter"
        );

        String source = Files.readString(adapter);

        assertTrue(
                source.contains(
                        "implements FundsReservationGateway"
                )
        );

        assertTrue(
                source.contains("@ConditionalOnBean")
        );

        assertTrue(
                source.contains(
                        "AmplitudeFundsReservationClient.class"
                )
        );
    }

    @Test
    void concreteHttpClientsAreRestrictedToApprovedPackages()
            throws IOException {

        List<String> forbiddenTokens = List.of(
                "RestClient",
                "WebClient",
                "HttpClient",
                "@ConfigurationProperties",
                "baseUrl"
        );

        try (Stream<Path> paths = Files.walk(ADAPTER_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            !isApprovedProviderInfrastructure(path)
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbiddenTokens.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(
                    List.of(),
                    violations
            );
        }
    }

    @Test
    void reservationClientNeverRetriesFinancialSideEffect()
            throws IOException {

        Path client = RESERVATION_ROOT.resolve(
                "client/RestAmplitudeFundsReservationClient.java"
        );

        assertTrue(
                Files.isRegularFile(client)
        );

        String source = Files.readString(client);

        for (String forbidden : List.of(
                "RetryingIntegrationExecutor",
                "IntegrationOperationType",
                "RetryTemplate",
                "@Retryable",
                "while (",
                "for (int attempt"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden retry mechanism: "
                            + forbidden
            );
        }

        assertTrue(
                source.contains(
                        "FundsReservationOutcomeUnknownException"
                )
        );

        assertTrue(
                source.contains(
                        "status == 429 || status >= 500"
                )
        );
    }

    @Test
    void reservationClientRequiresIdempotencyKey()
            throws IOException {

        Path client = RESERVATION_ROOT.resolve(
                "client/RestAmplitudeFundsReservationClient.java"
        );

        String source = Files.readString(client);

        assertTrue(
                source.contains(
                        "properties.contract().idempotencyHeader()"
                )
        );

        assertTrue(
                source.contains(
                        "request.idempotencyKey().toString()"
                )
        );
    }

    @Test
    void reservationClientDoesNotImplementLaterCapabilities()
            throws IOException {

        String sources = readAllJavaSources(
                RESERVATION_ROOT
        );

        for (String forbidden : List.of(
                "postPayment(",
                "findPostingByIdempotencyKey(",
                "findPostingByBankReference(",
                "reversePayment(",
                "releaseFunds(",
                "cancelReservation("
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Capability belongs to a later sub-lot: "
                            + forbidden
            );
        }
    }

    private static void assertAdapterConditionalOn(
            String fileName,
            String expectedClient
    ) throws IOException {

        Path adapter = ADAPTER_ROOT.resolve(fileName);

        if (!Files.isRegularFile(adapter)) {
            return;
        }

        String source = Files.readString(adapter);

        assertTrue(
                source.contains("@ConditionalOnBean"),
                () -> fileName
                        + " must declare @ConditionalOnBean"
        );

        assertTrue(
                source.contains(expectedClient + ".class"),
                () -> fileName
                        + " must be conditional on "
                        + expectedClient
        );

        assertTrue(
                source.contains(expectedClient),
                () -> fileName
                        + " must depend on "
                        + expectedClient
        );
    }

    private static boolean isApprovedProviderInfrastructure(
            Path path
    ) {
        String normalized = path
                .toString()
                .replace('\\', '/');

        return normalized.contains(
                "/infrastructure/banking/amplitude/client/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/configuration/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/reservation/client/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/reservation/configuration/"
        );
    }

    private static String readAllJavaSources(
            Path root
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return "";
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    "Cannot read " + path,
                                    exception
                            );
                        }
                    })
                    .reduce(
                            "",
                            (left, right) ->
                                    left + "\n" + right
                    );
        }
    }
}
