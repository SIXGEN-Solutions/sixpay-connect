package com.sixpay.customer.verification.infrastructure.banking.configuration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationPropertiesTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory()
                    .getValidator();

    @Test
    void acceptsSecureBoundedConfiguration() {
        BankingVerificationProperties properties =
                validProperties();

        assertTrue(
                validator.validate(properties).isEmpty()
        );
    }

    @Test
    void rejectsHttpBaseUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties(
                        URI.create(
                                "http://core-banking.internal"
                        ),
                        "/v1/accounts/verify",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        3,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        validSecurity(),
                        validContract()
                )
        );
    }

    @Test
    void rejectsEndpointPathWithoutLeadingSlash() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties(
                        URI.create(
                                "https://core-banking.internal"
                        ),
                        "accounts/verify",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        3,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        validSecurity(),
                        validContract()
                )
        );
    }

    @Test
    void rejectsNonPositiveTimeouts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties(
                        URI.create(
                                "https://core-banking.internal"
                        ),
                        "/v1/accounts/verify",
                        Duration.ZERO,
                        Duration.ofMillis(1),
                        3,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        validSecurity(),
                        validContract()
                )
        );
    }

    @Test
    void rejectsReadTimeoutShorterThanConnectTimeout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties(
                        URI.create(
                                "https://core-banking.internal"
                        ),
                        "/v1/accounts/verify",
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(2),
                        3,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        validSecurity(),
                        validContract()
                )
        );
    }

    @Test
    void beanValidationRejectsInvalidMaximumAttempts() {
        BankingVerificationProperties properties =
                new BankingVerificationProperties(
                        URI.create(
                                "https://core-banking.internal"
                        ),
                        "/v1/accounts/verify",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        6,
                        Duration.ofMillis(250),
                        Duration.ofMinutes(5),
                        validSecurity(),
                        validContract()
                );

        assertTrue(
                validator.validate(properties)
                        .stream()
                        .anyMatch(violation ->
                                "maxAttempts".equals(
                                        violation
                                                .getPropertyPath()
                                                .toString()
                                )
                        )
        );
    }

    @Test
    void rejectsBlankSecurityValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties.Security(
                        " ",
                        " "
                )
        );
    }

    @Test
    void rejectsEmptySuccessCodeSet() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BankingVerificationProperties.Contract(
                        "test-v1",
                        Set.of(),
                        Set.of(
                                "01",
                                "02"
                        )
                )
        );
    }

    private static BankingVerificationProperties
    validProperties() {
        return new BankingVerificationProperties(
                URI.create(
                        "https://core-banking.internal"
                ),
                "/v1/accounts/verify",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                3,
                Duration.ofMillis(250),
                Duration.ofMinutes(5),
                validSecurity(),
                validContract()
        );
    }

    private static BankingVerificationProperties.Security
    validSecurity() {
        return new BankingVerificationProperties.Security(
                "core-banking-customer-verification",
                "core-banking-client"
        );
    }

    private static BankingVerificationProperties.Contract
    validContract() {
        return new BankingVerificationProperties.Contract(
                "test-v1",
                Set.of(
                        "00",
                        "200"
                ),
                Set.of(
                        "01",
                        "02",
                        "03",
                        "04"
                )
        );
    }
}