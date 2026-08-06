package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class AmplitudeResponseValidatorTest {

    private final AmplitudeResponseValidator validator =
            new AmplitudeResponseValidator(properties());

    @Test
    void acceptsKnownSuccessCode() {
        assertThat(
                validator.validate(response("00"))
        ).isNotNull();
    }

    @Test
    void acceptsKnownBusinessFailureCode() {
        assertThat(
                validator.validate(response("03"))
        ).isNotNull();
    }

    @Test
    void rejectsUnknownFunctionalCode() {
        assertThatThrownBy(() ->
                validator.validate(response("999"))
        ).isInstanceOf(
                AmplitudeInvalidResponseException.class
        );
    }

    private static AmplitudeCustomerVerificationResponse response(
            String code
    ) {
        return new AmplitudeCustomerVerificationResponse(
                code,
                true,
                "ACTIVE",
                "Customer",
                "****0001",
                "XAF",
                BigDecimal.TEN,
                BigDecimal.TEN,
                true,
                "Verified",
                "00".equals(code)
                        ? "SUCCESS"
                        : "FAILURE",
                Instant.parse("2026-08-06T14:00:00Z"),
                null,
                Map.of(
                        "CUSTOMER_EXISTS",
                        "00".equals(code) ? "PASS" : "FAIL"
                )
        );
    }

    private static BankingVerificationProperties properties() {
        return new BankingVerificationProperties(
                URI.create("https://amplitude.test"),
                "/v1/accounts/verify",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                3,
                Duration.ofMillis(10),
                Duration.ofMinutes(5),
                new BankingVerificationProperties.Security(
                        "registration",
                        "ssl-bundle"
                ),
                new BankingVerificationProperties.Contract(
                        "provisional-v1",
                        Set.of("00"),
                        Set.of("01", "02", "03", "04")
                )
        );
    }
}
