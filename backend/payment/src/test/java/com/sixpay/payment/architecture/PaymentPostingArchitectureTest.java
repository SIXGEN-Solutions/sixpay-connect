package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPostingArchitectureTest {

    private static final Path POSTING = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude/posting"
    );

    @Test
    void genericPostingAdapterIsRemoved() {

        Path genericAdapter = Path.of(
                "src/main/java/com/sixpay/payment/"
                        + "infrastructure/banking/amplitude/"
                        + "AmplitudePostingAdapter.java"
        );

        assertFalse(
                Files.exists(genericAdapter)
        );
    }

    @Test
    void dedicatedAdapterUsesConcretePostingClient()
            throws Exception {

        String source = Files.readString(
                POSTING.resolve(
                        "DedicatedAmplitudePostingAdapter.java"
                )
        );

        assertTrue(
                source.contains("AmplitudePostingClient")
        );
        assertTrue(
                source.contains(
                        "@ConditionalOnMissingBean(PostingGateway.class)"
                )
        );
        assertFalse(
                source.contains(
                        "Amplitude" + "BankingClient"
                )
        );
    }

    @Test
    void postingClientNeverRetriesBlindly()
            throws Exception {

        String source = Files.readString(
                POSTING.resolve(
                        "client/RestAmplitudePostingClient.java"
                )
        );

        for (String forbidden : new String[]{
                "RetryingIntegrationExecutor",
                "IntegrationOperationType",
                "@Retryable",
                "RetryTemplate"
        }) {
            assertFalse(source.contains(forbidden));
        }

        assertTrue(
                source.contains(
                        "PostingOutcomeUnknownException"
                )
        );
        assertTrue(
                source.contains(
                        "status == 429 || status >= 500"
                )
        );
    }

    @Test
    void postingRequiresIdempotencyHeader()
            throws Exception {

        String source = Files.readString(
                POSTING.resolve(
                        "client/RestAmplitudePostingClient.java"
                )
        );

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
}
