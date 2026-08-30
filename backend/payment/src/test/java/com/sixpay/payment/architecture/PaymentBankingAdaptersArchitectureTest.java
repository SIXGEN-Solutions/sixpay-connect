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

    private static final Path AMPLITUDE_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude"
    );

    private static final Path RESERVATION_ROOT =
            AMPLITUDE_ROOT.resolve("reservation");

    private static final Path POSTING_ROOT =
            AMPLITUDE_ROOT.resolve("posting");

    private static final Path RELEASE_ROOT =
            AMPLITUDE_ROOT.resolve("release");

    private static final Path REVERSAL_ROOT =
            AMPLITUDE_ROOT.resolve("reversal");

    private static final Path STATUS_ROOT =
            AMPLITUDE_ROOT.resolve("status");

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
                "PaymentConfirmationBankResult.java",
                "PaymentConfirmationGateway.java",
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
    void accountAndFundsAdaptersUseNarrowClient()
            throws IOException {

        for (String adapterName : List.of(
                "AmplitudeVerificationAdapter.java",
                "AmplitudeFundsAdapter.java"
        )) {
            Path adapter = AMPLITUDE_ROOT.resolve(adapterName);
            String source = normalizeSource(
                    readRequiredSource(adapter)
            );

            assertTrue(
                    source.contains(
                            "@ConditionalOnBean("
                                    + "AmplitudeAccountFundsClient.class)"
                    )
            );
            assertTrue(
                    source.contains("AmplitudeAccountFundsClient")
            );
        }

        for (String removedAdapter : List.of(
                "AmplitudePostingAdapter.java",
                "AmplitudeLookupAdapter.java",
                "AmplitudeReversalAdapter.java"
        )) {
            assertFalse(
                    Files.exists(
                            AMPLITUDE_ROOT.resolve(removedAdapter)
                    ),
                    () -> removedAdapter
                            + " must be removed after CB-2 consolidation"
            );
        }

        assertFalse(
                Files.exists(
                        AMPLITUDE_ROOT.resolve(
                                "Amplitude"
                                        + "BankingClient.java"
                        )
                ),
                "Generic Amplitude banking facade must not exist"
        );
    }

    @Test
    void dedicatedAdaptersAreConditionalAndNonConflicting()
            throws IOException {

        assertDedicatedAdapter(
                RESERVATION_ROOT.resolve(
                        "AmplitudeFundsReservationAdapter.java"
                ),
                "FundsReservationGateway",
                "AmplitudeFundsReservationClient",
                false
        );

        assertDedicatedAdapter(
                POSTING_ROOT.resolve(
                        "DedicatedAmplitudePostingAdapter.java"
                ),
                "PostingGateway",
                "AmplitudePostingClient",
                true
        );

        assertDedicatedAdapter(
                RELEASE_ROOT.resolve(
                        "DedicatedAmplitudeFundsReleaseAdapter.java"
                ),
                "FundsReleaseGateway",
                "AmplitudeFundsReleaseClient",
                true
        );

        assertDedicatedAdapter(
                REVERSAL_ROOT.resolve(
                        "DedicatedAmplitudeReversalAdapter.java"
                ),
                "ReversalGateway",
                "AmplitudeReversalClient",
                true
        );

        assertDedicatedAdapter(
                STATUS_ROOT.resolve(
                        "DedicatedAmplitudeLookupAdapter.java"
                ),
                "LookupGateway",
                "AmplitudePostingStatusClient",
                true
        );
    }

    @Test
    void providerHttpConceptsStayInApprovedPackages()
            throws IOException {

        List<String> forbiddenTokens = List.of(
                "RestClient",
                "WebClient",
                "HttpClient",
                "@ConfigurationProperties",
                "baseUrl"
        );

        try (Stream<Path> paths =
                     Files.walk(AMPLITUDE_ROOT)) {

            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            !isApprovedProviderInfrastructure(
                                    path
                            )
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
    void financialCommandsAreIdempotentAndNeverRetryBlindly()
            throws IOException {

        for (Path client : List.of(
                RESERVATION_ROOT.resolve(
                        "client/"
                                + "RestAmplitudeFundsReservationClient.java"
                ),
                POSTING_ROOT.resolve(
                        "client/"
                                + "RestAmplitudePostingClient.java"
                ),
                RELEASE_ROOT.resolve(
                        "client/"
                                + "RestAmplitudeFundsReleaseClient.java"
                ),
                REVERSAL_ROOT.resolve(
                        "client/"
                                + "RestAmplitudeReversalClient.java"
                )
        )) {
            String source = normalizeSource(
                    readRequiredSource(client)
            );

            assertTrue(
                    source.contains(
                            "properties.contract()"
                                    + ".idempotencyHeader()"
                    ),
                    () -> client
                            + " must use the configured "
                            + "idempotency header"
            );

            assertTrue(
                    source.contains(
                            "request.idempotencyKey()"
                                    + ".toString()"
                    ),
                    () -> client
                            + " must propagate the banking "
                            + "idempotency key"
            );

            assertFalse(
                    source.contains(
                            "RetryingIntegrationExecutor"
                    )
            );
            assertFalse(
                    source.contains("@Retryable")
            );
            assertFalse(
                    source.contains("RetryTemplate")
            );
        }
    }

    @Test
    void statusLookupIsReadOnlyAndDoesNotReplayCommands()
            throws IOException {

        String source = normalizeSource(
                readRequiredSource(
                        STATUS_ROOT.resolve(
                                "client/"
                                        + "RestAmplitudePostingStatusClient.java"
                        )
                )
        );

        assertTrue(
                source.contains("restClient.get()")
        );
        assertFalse(
                source.contains("restClient.post()")
        );
        assertFalse(
                source.contains("postPayment(")
        );
        assertFalse(
                source.contains("reversePayment(")
        );

        assertTrue(
                source.contains(
                        "EvidenceObservationChannel."
                                + "IDEMPOTENCY_LOOKUP"
                )
        );

        assertTrue(
                source.contains(
                        "EvidenceObservationChannel."
                                + "BANK_REFERENCE_LOOKUP"
                )
        );
    }

    private static void assertDedicatedAdapter(
            Path adapter,
            String gatewayType,
            String clientType,
            boolean requireMissingBeanGuard
    ) throws IOException {

        String source = normalizeSource(
                readRequiredSource(adapter)
        );

        assertTrue(
                source.contains(
                        "implements" + gatewayType
                )
        );

        assertTrue(
                source.contains(
                        "@ConditionalOnBean("
                                + clientType
                                + ".class)"
                )
        );

        if (requireMissingBeanGuard) {
            assertTrue(
                    source.contains(
                            "@ConditionalOnMissingBean("
                                    + gatewayType
                                    + ".class)"
                    )
            );
        }
    }

    private static boolean
    isApprovedProviderInfrastructure(
            Path path
    ) {
        String normalized = path.toString()
                .replace('\\', '/');

        return normalized.contains(
                "/amplitude/client/"
        )
                || normalized.contains(
                "/amplitude/configuration/"
        )
                || normalized.contains(
                "/amplitude/reservation/client/"
        )
                || normalized.contains(
                "/amplitude/reservation/configuration/"
        )
                || normalized.contains(
                "/amplitude/posting/client/"
        )
                || normalized.contains(
                "/amplitude/posting/configuration/"
        )
                || normalized.contains(
                "/amplitude/release/client/"
        )
                || normalized.contains(
                "/amplitude/release/configuration/"
        )
                || normalized.contains(
                "/amplitude/reversal/client/"
        )
                || normalized.contains(
                "/amplitude/reversal/configuration/"
        )
                || normalized.contains(
                "/amplitude/compensation/"
        )
                || normalized.contains(
                "/amplitude/status/client/"
        )
                || normalized.contains(
                "/amplitude/status/configuration/"
        );
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
}
