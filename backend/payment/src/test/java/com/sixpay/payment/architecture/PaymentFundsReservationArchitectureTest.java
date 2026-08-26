package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PaymentFundsReservationArchitectureTest {

    private static final Path RESERVATION = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/banking/amplitude/reservation"
    );

    @Test
    void reservationUsesDedicatedGatewayAndClient()
            throws Exception {
        String adapter = Files.readString(
                RESERVATION.resolve(
                        "AmplitudeFundsReservationAdapter.java"
                )
        );

        assertTrue(
                adapter.contains(
                        "implements FundsReservationGateway"
                )
        );
        assertTrue(
                adapter.contains(
                        "AmplitudeFundsReservationClient"
                )
        );
    }

    @Test
    void reservationClientNeverUsesRetryExecutor()
            throws Exception {
        String client = Files.readString(
                RESERVATION.resolve(
                        "client/RestAmplitudeFundsReservationClient.java"
                )
        );

        assertFalse(
                client.contains(
                        "RetryingIntegrationExecutor"
                )
        );
        assertFalse(
                client.contains(
                        "IntegrationOperationType"
                )
        );
        assertTrue(
                client.contains(
                        "FundsReservationOutcomeUnknownException"
                )
        );
    }
}
