package com.sixpay.payment.infrastructure.banking.amplitude.validation;

import com.sixpay.payment.infrastructure.banking.amplitude.configuration.AmplitudePaymentBankingProperties;
import com.sixpay.payment.infrastructure.banking.amplitude.dto.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AmplitudeAccountFundsResponseValidatorTest {

    private final AmplitudeAccountFundsResponseValidator validator =
            new AmplitudeAccountFundsResponseValidator(
                    properties()
            );

    @Test
    void acceptsAccountOppositionResponse() {
        assertDoesNotThrow(() -> validator.validate(
                new AmplitudeAccountVerificationResponse(
                        "00",
                        "00000000-0000-0000-0000-000000000001",
                        "VERIFIED",
                        "v1:" + "a".repeat(64),
                        Map.of(
                                "ACCOUNT_EXISTS",
                                new AmplitudeCheckResult(
                                        "PASS",
                                        null,
                                        Instant.parse(
                                                "2026-08-06T16:00:00Z"
                                        )
                                ),
                                "ACCOUNT_NOT_OPPOSED",
                                new AmplitudeCheckResult(
                                        "PASS",
                                        null,
                                        Instant.parse(
                                                "2026-08-06T16:00:00Z"
                                        )
                                )
                        ),
                        Instant.parse("2026-08-06T16:00:00Z")
                )
        ));
    }

    @Test
    void acceptsInsufficientFundsAsBusinessRejection() {
        assertDoesNotThrow(() -> validator.validate(
                new AmplitudeFundsCheckResponse(
                        "04",
                        "FUNDS-VERIFICATION-0001",
                        "REJECTED",
                        new BigDecimal("600000"),
                        "XAF",
                        "v1:" + "b".repeat(64),
                        Map.of(
                                "AVAILABLE_FUNDS_SUFFICIENT",
                                new AmplitudeCheckResult(
                                        "FAIL",
                                        "INSUFFICIENT_FUNDS",
                                        Instant.parse(
                                                "2026-08-06T16:00:00Z"
                                        )
                                )
                        ),
                        Instant.parse("2026-08-06T16:00:00Z"),
                        Instant.parse("2026-08-06T16:05:00Z")
                )
        ));
    }

    @Test
    void rejectsUnknownProviderCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        new AmplitudeFundsCheckResponse(
                                "999",
                                "FUNDS-VERIFICATION-0001",
                                "INDETERMINATE",
                                BigDecimal.ONE,
                                "XAF",
                                "v1:" + "b".repeat(64),
                                Map.of(
                                        "AVAILABLE_FUNDS_SUFFICIENT",
                                        new AmplitudeCheckResult(
                                                "UNKNOWN",
                                                "PROVIDER_UNKNOWN",
                                                Instant.now()
                                        )
                                ),
                                Instant.now(),
                                Instant.now().plusSeconds(60)
                        )
                )
        );
    }

    private static AmplitudePaymentBankingProperties properties() {
        return new AmplitudePaymentBankingProperties(
                URI.create("https://amplitude.internal"),
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
