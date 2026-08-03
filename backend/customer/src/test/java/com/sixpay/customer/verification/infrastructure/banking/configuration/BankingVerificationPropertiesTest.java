package com.sixpay.customer.verification.infrastructure.banking.configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationPropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory()
                    .getValidator();

    @Test
    void acceptsSecureBoundedConfiguration() {
        BankingVerificationProperties properties =
                validProperties();

        assertTrue(validator.validate(properties).isEmpty());
        assertTrue(properties.isValid());
    }

    @Test
    void rejectsHttpNonPositiveTimeoutsAndInvalidAttempts() {
        BankingVerificationProperties properties =
                new BankingVerificationProperties(
                        URI.create("http://core-banking.internal"),
                        "accounts/verify",
                        Duration.ZERO,
                        Duration.ofMillis(1),
                        6,
                        Duration.ofMillis(-1),
                        Duration.ZERO,
                        new BankingVerificationProperties.Security(
                                " ",
                                " "
                        )
                );

        assertFalse(validator.validate(properties).isEmpty());
        assertFalse(properties.isValid());
    }

    @Test
    void rejectsReadTimeoutShorterThanConnectTimeout() {
        BankingVerificationProperties properties =
                new BankingVerificationProperties(
                        URI.create("https://core-banking.internal"),
                        "/v1/accounts/verify",
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(2),
                        3,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        new BankingVerificationProperties.Security(
                                "core-banking",
                                "core-banking-client"
                        )
                );

        assertFalse(properties.isValid());
    }

    private static BankingVerificationProperties validProperties() {
        return new BankingVerificationProperties(
                URI.create("https://core-banking.internal"),
                "/v1/accounts/verify",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                3,
                Duration.ofMillis(250),
                Duration.ofMinutes(5),
                new BankingVerificationProperties.Security(
                        "core-banking-customer-verification",
                        "core-banking-client"
                )
        );
    }
}
