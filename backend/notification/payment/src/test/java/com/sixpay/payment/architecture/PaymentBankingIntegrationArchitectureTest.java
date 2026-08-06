package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PaymentBankingIntegrationArchitectureTest {

    private static final Path AMPLITUDE = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude"
    );

    @Test
    void accountAndFundsAdaptersUseNarrowClient() throws Exception {
        for (String sourceName : new String[]{
                "AmplitudeVerificationAdapter.java",
                "AmplitudeFundsAdapter.java"
        }) {
            String source = Files.readString(
                    AMPLITUDE.resolve(sourceName)
            );

            assertTrue(
                    source.contains("AmplitudeAccountFundsClient")
            );
            assertFalse(
                    source.contains(
                            "private final AmplitudeBankingClient"
                    )
            );
        }
    }

    @Test
    void lotDoesNotImplementPostingOrReversal() throws Exception {
        String source = Files.readString(
                AMPLITUDE.resolve(
                        "AmplitudeAccountFundsClient.java"
                )
        );

        assertFalse(source.contains("postPayment("));
        assertFalse(source.contains("reversePayment("));
        assertFalse(source.contains("findPosting"));
    }
}
