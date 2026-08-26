package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.verification.application.exception.BankingVerificationTimeoutException;
import com.sixpay.customer.verification.application.exception.BankingVerificationUnavailableException;
import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.payment.application.port.output.CustomerVerificationRequest;
import com.sixpay.payment.application.port.output.CustomerVerificationTechnicalException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerVerificationModuleAdapterTechnicalFailureTest {

    @Test
    void timeoutIsTranslatedToPaymentOwnedRetryableFailure() {
        VerifyCustomerUseCase useCase = command -> {
            throw new BankingVerificationTimeoutException(
                    "timeout",
                    new RuntimeException("socket timeout")
            );
        };

        var failure = assertThrows(
                CustomerVerificationTechnicalException.class,
                () -> new CustomerVerificationModuleAdapter(useCase)
                        .verify(request())
        );

        assertEquals(
                CustomerVerificationTechnicalException.ErrorType.TIMEOUT,
                failure.errorType()
        );
        assertEquals(request().verificationId(), failure.verificationId());
        assertTrue(failure.retryable());
    }

    @Test
    void unavailableIsTranslatedToPaymentOwnedRetryableFailure() {
        VerifyCustomerUseCase useCase = command -> {
            throw new BankingVerificationUnavailableException(
                    "unavailable",
                    new RuntimeException("503")
            );
        };

        var failure = assertThrows(
                CustomerVerificationTechnicalException.class,
                () -> new CustomerVerificationModuleAdapter(useCase)
                        .verify(request())
        );

        assertEquals(
                CustomerVerificationTechnicalException.ErrorType.UNAVAILABLE,
                failure.errorType()
        );
        assertTrue(failure.retryable());
    }

    private static CustomerVerificationRequest request() {
        return new CustomerVerificationRequest(
                UUID.fromString(
                        "f85d7f62-c092-3889-8eca-f3f39d19a288"
                ),
                "M0123456",
                "Ada Lovelace",
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "AMP-ACC-000123",
                "c74e165f-df46-463e-a520-188e6df3e5ae",
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                Instant.parse("2026-08-03T20:30:00Z")
        );
    }
}
