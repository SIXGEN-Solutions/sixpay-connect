package com.sixpay.customer.verification.configuration;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationPropertiesTest {
    @Test
    void acceptsSecureBoundedConfiguration() {
        var p = new BankingVerificationProperties(
                URI.create("https://core-banking.internal"),
                "/v1/accounts/verify",
                Duration.ofSeconds(2), Duration.ofSeconds(5), 3,
                Duration.ofMillis(250), Duration.ofMinutes(5),
                new BankingVerificationProperties.Security("core-banking", "core-banking-client")
        );
        assertTrue(Validation.buildDefaultValidatorFactory().getValidator().validate(p).isEmpty());
        assertTrue(p.isValid());
    }

    @Test
    void rejectsInsecureOrInconsistentConfiguration() {
        var p = new BankingVerificationProperties(
                URI.create("http://core-banking.internal"),
                "accounts/verify",
                Duration.ZERO, Duration.ofMillis(1), 6,
                Duration.ofMillis(-1), Duration.ZERO,
                new BankingVerificationProperties.Security(" ", " ")
        );
        assertFalse(p.isValid());
    }
}
