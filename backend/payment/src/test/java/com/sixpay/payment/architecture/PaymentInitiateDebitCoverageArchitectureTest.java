package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentInitiateDebitCoverageArchitectureTest {

    private static final Path TEST_ROOT =
            Path.of("src/test/java/com/sixpay/payment");

    @Test
    void commandApiIdempotencyPreparationAndCallbackHaveTests() {
        List<String> required = List.of(
                "application/command/"
                        + "InitiateDebitCommandTest.java",
                "application/service/"
                        + "PaymentInitiationOrchestrationServiceTest.java",
                "api/PaymentCommandApiMapperTest.java",
                "infrastructure/initiation/"
                        + "PaymentInitiationPreparationAdapterTest.java",
                "infrastructure/idempotency/"
                        + "PaymentInitiationCanonicalizerTest.java",
                "infrastructure/idempotency/"
                        + "PaymentInitiationReplayCodecTest.java",
                "infrastructure/callback/relay/"
                        + "PaymentCallbackOutboxRelayTest.java",
                "architecture/"
                        + "PaymentAsyncCallbackArchitectureTest.java"
        );

        required.forEach(relative ->
                assertTrue(
                        Files.isRegularFile(
                                TEST_ROOT.resolve(relative)
                        ),
                        () -> "Missing InitiateDebit test: "
                                + relative
                )
        );
    }
}
