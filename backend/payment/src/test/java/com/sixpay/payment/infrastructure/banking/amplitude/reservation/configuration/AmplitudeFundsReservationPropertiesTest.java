package com.sixpay.payment.infrastructure.banking.amplitude.reservation.configuration;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AmplitudeFundsReservationPropertiesTest {

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

    private static AmplitudeFundsReservationProperties properties(
            URI baseUrl
    ) {
        return new AmplitudeFundsReservationProperties(
                baseUrl,
                "/v1/payment/funds/reservations",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                new AmplitudeFundsReservationProperties.Security(
                        "amplitude-payment",
                        "amplitude-payment-client"
                ),
                new AmplitudeFundsReservationProperties.Contract(
                        "test-v1",
                        Set.of("00"),
                        Set.of("02", "03", "04", "05"),
                        "Idempotency-Key"
                )
        );
    }
}
