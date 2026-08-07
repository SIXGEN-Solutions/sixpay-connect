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

    private static final Path POSTING_ROOT =
            ADAPTER_ROOT.resolve("posting");

    private static final Path RELEASE_ROOT =
            ADAPTER_ROOT.resolve("release");

    private static final Path REVERSAL_ROOT =
            ADAPTER_ROOT.resolve("reversal");

    @Test
    void exposesOnlyApprovedBankingGateways()
            throws IOException {

        Set<String> expected = Set.of(
                "BankingIdempotencyKey.java",
                "BankingRequestContext.java",
                "FundsGateway.java",
                "FundsReleaseGateway.java",
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
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .sorted()
                    .toList();

            assertEquals(
                    expected.stream()
                            .sorted()
                            .toList(),
                    actual
            );
        }
    }

    @Test
    void reservationAndReleaseCapabilitiesAreExplicitlyLimited()
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

        assertTrue(
                sources.contains(
                        "interface FundsReleaseGateway"
                )
        );

        assertTrue(
                sources.contains(
                        "FundsReleaseSnapshot release("
                )
        );

        for (String forbidden : List.of(
                "cancelReservation",
                "lookupReservation",
                "retryReservation",
                "retryPosting",
                "automaticReversal"
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Unapproved banking capability: "
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

        assertDedicatedAdapter(
                RESERVATION_ROOT.resolve(
                        "AmplitudeFundsReservationAdapter.java"
                ),
                "FundsReservationGateway",
                "AmplitudeFundsReservationClient",
                false
        );
    }

    @Test
    void dedicatedPostingAdapterIsConditionalAndNonConflicting()
            throws IOException {

        assertDedicatedAdapter(
                POSTING_ROOT.resolve(
                        "DedicatedAmplitudePostingAdapter.java"
                ),
                "PostingGateway",
                "AmplitudePostingClient",
                true
        );
    }

    @Test
    void dedicatedReleaseAdapterIsConditionalAndNonConflicting()
            throws IOException {

        assertDedicatedAdapter(
                RELEASE_ROOT.resolve(
                        "DedicatedAmplitudeFundsReleaseAdapter.java"
                ),
                "FundsReleaseGateway",
                "AmplitudeFundsReleaseClient",
                true
        );
    }

    @Test
    void dedicatedReversalAdapterIsConditionalAndNonConflicting()
            throws IOException {

        assertDedicatedAdapter(
                REVERSAL_ROOT.resolve(
                        "DedicatedAmplitudeReversalAdapter.java"
                ),
                "ReversalGateway",
                "AmplitudeReversalClient",
                true
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

        String source = readRequiredSource(
                RESERVATION_ROOT.resolve(
                        "client/"
                                + "RestAmplitudeFundsReservationClient.java"
                )
        );

        assertNoRetryMechanism(source);

        assertTrue(
                source.contains(
                        "FundsReservationOutcomeUnknownException"
                )
        );

        assertTrue(
                normalizeSource(source).contains(
                        "status==429||status>=500"
                )
        );
    }

    @Test
    void postingClientNeverRetriesFinancialSideEffect()
            throws IOException {

        String source = readRequiredSource(
                POSTING_ROOT.resolve(
                        "client/RestAmplitudePostingClient.java"
                )
        );

        assertNoRetryMechanism(source);

        assertTrue(
                source.contains(
                        "PostingOutcomeUnknownException"
                )
        );

        assertTrue(
                normalizeSource(source).contains(
                        "status==429||status>=500"
                )
        );
    }

    @Test
    void releaseClientNeverRetriesFinancialSideEffect()
            throws IOException {

        String source = readRequiredSource(
                RELEASE_ROOT.resolve(
                        "client/RestAmplitudeFundsReleaseClient.java"
                )
        );

        assertNoRetryMechanism(source);

        assertTrue(
                source.contains(
                        "Funds release outcome is unknown"
                )
        );

        assertTrue(
                normalizeSource(source).contains(
                        "status==429||status>=500"
                )
        );
    }

    @Test
    void reversalClientNeverRetriesFinancialSideEffect()
            throws IOException {

        String source = readRequiredSource(
                REVERSAL_ROOT.resolve(
                        "client/RestAmplitudeReversalClient.java"
                )
        );

        assertNoRetryMechanism(source);

        assertTrue(
                source.contains(
                        "Reversal outcome is unknown"
                )
        );

        assertTrue(
                normalizeSource(source).contains(
                        "status==429||status>=500"
                )
        );
    }

    @Test
    void financialCommandsRequireIdempotencyKeys()
            throws IOException {

        List<Path> clients = List.of(
                RESERVATION_ROOT.resolve(
                        "client/"
                                + "RestAmplitudeFundsReservationClient.java"
                ),
                POSTING_ROOT.resolve(
                        "client/RestAmplitudePostingClient.java"
                ),
                RELEASE_ROOT.resolve(
                        "client/RestAmplitudeFundsReleaseClient.java"
                ),
                REVERSAL_ROOT.resolve(
                        "client/RestAmplitudeReversalClient.java"
                )
        );

        for (Path client : clients) {
            String normalizedSource = normalizeSource(
                    readRequiredSource(client)
            );

            assertTrue(
                    normalizedSource.contains(
                            "properties.contract()"
                                    + ".idempotencyHeader()"
                    ),
                    () -> client
                            + " must use the configured "
                            + "idempotency header"
            );

            assertTrue(
                    normalizedSource.contains(
                            "request.idempotencyKey()"
                                    + ".toString()"
                    ),
                    () -> client
                            + " must propagate the banking "
                            + "idempotency key"
            );
        }
    }

    @Test
    void subLotsDoNotImplementUnapprovedCrossCapabilities()
            throws IOException {

        String reservationSources =
                readAllJavaSources(RESERVATION_ROOT);

        String postingSources =
                readAllJavaSources(POSTING_ROOT);

        String releaseSources =
                readAllJavaSources(RELEASE_ROOT);

        assertFalse(
                reservationSources.contains(
                        "postPayment("
                )
        );

        assertFalse(
                reservationSources.contains(
                        "reversePayment("
                )
        );

        assertFalse(
                postingSources.contains(
                        "release("
                )
        );

        assertFalse(
                releaseSources.contains(
                        "reversePayment("
                )
        );

        String allFinancialCommandSources =
                reservationSources
                        + postingSources
                        + releaseSources
                        + readAllJavaSources(
                        REVERSAL_ROOT
                );

        for (String forbidden : List.of(
                "findPostingByIdempotencyKey(",
                "findPostingByBankReference(",
                "automaticRetry(",
                "automaticCompensation("
        )) {
            assertFalse(
                    allFinancialCommandSources.contains(
                            forbidden
                    ),
                    () -> "Capability belongs to a later "
                            + "reconciliation sub-lot: "
                            + forbidden
            );
        }
    }

    private static void assertDedicatedAdapter(
            Path adapter,
            String gatewayType,
            String clientType,
            boolean requireMissingBeanGuard
    ) throws IOException {

        assertTrue(
                Files.isRegularFile(adapter),
                () -> "Missing adapter: " + adapter
        );

        String normalizedSource = normalizeSource(
                Files.readString(adapter)
        );

        assertTrue(
                normalizedSource.contains(
                        "implements" + gatewayType
                ),
                () -> adapter
                        + " must implement "
                        + gatewayType
        );

        assertTrue(
                normalizedSource.contains(
                        "@ConditionalOnBean("
                                + clientType
                                + ".class)"
                ),
                () -> adapter
                        + " must be conditional on "
                        + clientType
        );

        if (requireMissingBeanGuard) {
            assertTrue(
                    normalizedSource.contains(
                            "@ConditionalOnMissingBean("
                                    + gatewayType
                                    + ".class)"
                    ),
                    () -> adapter
                            + " must not conflict with an existing "
                            + gatewayType
            );
        }
    }

    private static void assertAdapterConditionalOn(
            String fileName,
            String expectedClient
    ) throws IOException {

        Path adapter = ADAPTER_ROOT.resolve(
                fileName
        );

        if (!Files.isRegularFile(adapter)) {
            return;
        }

        String normalizedSource = normalizeSource(
                Files.readString(adapter)
        );

        assertTrue(
                normalizedSource.contains(
                        "@ConditionalOnBean("
                                + expectedClient
                                + ".class)"
                ),
                () -> fileName
                        + " must be conditional on "
                        + expectedClient
        );

        assertTrue(
                normalizedSource.contains(expectedClient),
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
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/posting/client/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/posting/configuration/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/release/client/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/release/configuration/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/reversal/client/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/reversal/configuration/"
        )
                || normalized.contains(
                "/infrastructure/banking/amplitude/compensation/"
        );
    }

    private static void assertNoRetryMechanism(
            String source
    ) {
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
    }

    private static String normalizeSource(
            String source
    ) {
        return source
                .replace(" ", "")
                .replace("\t", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static String readRequiredSource(
            Path path
    ) throws IOException {

        assertTrue(
                Files.isRegularFile(path),
                () -> "Missing source: " + path
        );

        return Files.readString(path);
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
                            path.toString()
                                    .endsWith(".java")
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
