package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PaymentCompensationArchitectureTest {

    private static final Path AMPLITUDE = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude"
    );

    @Test
    void genericReversalAdapterIsRemoved() {
        assertFalse(
                Files.exists(
                        AMPLITUDE.resolve(
                                "AmplitudeReversalAdapter.java"
                        )
                )
        );

        assertFalse(
                Files.exists(
                        AMPLITUDE.resolve(
                                "Amplitude"
                                        + "BankingClient.java"
                        )
                )
        );
    }

    @Test
    void dedicatedCompensationAdaptersDoNotConflict()
            throws Exception {
        String reversal = Files.readString(
                AMPLITUDE.resolve(
                        "reversal/DedicatedAmplitudeReversalAdapter.java"
                )
        );
        String release = Files.readString(
                AMPLITUDE.resolve(
                        "release/DedicatedAmplitudeFundsReleaseAdapter.java"
                )
        );

        assertTrue(
                reversal.contains(
                        "@ConditionalOnMissingBean(ReversalGateway.class)"
                )
        );
        assertTrue(
                release.contains(
                        "@ConditionalOnMissingBean(FundsReleaseGateway.class)"
                )
        );
    }

    @Test
    void compensationCommandsNeverRetryBlindly()
            throws Exception {
        String reversal = Files.readString(
                AMPLITUDE.resolve(
                        "reversal/client/RestAmplitudeReversalClient.java"
                )
        );
        String release = Files.readString(
                AMPLITUDE.resolve(
                        "release/client/RestAmplitudeFundsReleaseClient.java"
                )
        );

        for (String source : new String[]{reversal, release}) {
            assertFalse(source.contains("@Retryable"));
            assertFalse(source.contains("RetryTemplate"));
            assertFalse(
                    source.contains(
                            "RetryingIntegrationExecutor"
                    )
            );
            assertTrue(
                    source.contains(
                            "status == 429 || status >= 500"
                    )
            );
            assertTrue(
                    source.contains(
                            "request.idempotencyKey().toString()"
                    )
            );
        }
    }
}
