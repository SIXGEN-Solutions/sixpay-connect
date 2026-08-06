package com.sixpay.payment.infrastructure.banking.amplitude.configuration;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AmplitudePaymentBankingPropertiesTest {

    @Test
    void acceptsSecureConfiguration() {
        assertDoesNotThrow(() -> properties(
                URI.create("https://amplitude.internal")
        ));
    }

    @Test
    void acceptsLoopbackHttpForTests() {
        assertDoesNotThrow(() -> properties(
                URI.create("http://127.0.0.1:18080")
        ));
    }

    @Test
    void rejectsRemoteHttp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> properties(
                        URI.create("http://amplitude.internal")
                )
        );
    }

    private static AmplitudePaymentBankingProperties properties(
            URI baseUrl
    ) {
        return new AmplitudePaymentBankingProperties(
                baseUrl,
                "/v1/payment/accounts/verify",
                "/v1/payment/accounts/funds-check",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                new AmplitudePaymentBankingProperties.Security(
                        "amplitude-payment",
                        "amplitude-payment-client"
                ),
                new AmplitudePaymentBankingProperties.Contract(
                        "test-v1",
                        Set.of("00"),
                        Set.of("01", "02", "03", "04")
                )
        );
    }
}
