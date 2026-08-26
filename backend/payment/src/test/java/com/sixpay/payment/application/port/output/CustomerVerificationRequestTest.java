package com.sixpay.payment.application.port.output;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerVerificationRequestTest {

    @Test
    void rejectsMissingMandatoryData() {
        CustomerVerificationRequest valid = validRequest();

        assertThrows(
                NullPointerException.class,
                () -> new CustomerVerificationRequest(
                        null,
                        valid.customerNiu(),
                        valid.customerLegalName(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.integrationAccountToken(),
                        valid.correlationId(),
                        valid.causationId(),
                        valid.requestedAt()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new CustomerVerificationRequest(
                        valid.verificationId(),
                        " ",
                        valid.customerLegalName(),
                        valid.financialInstitutionCode(),
                        valid.accountBindingFingerprint(),
                        valid.integrationAccountToken(),
                        valid.correlationId(),
                        valid.causationId(),
                        valid.requestedAt()
                )
        );
    }

    @Test
    void toStringRedactsSensitiveValues() {
        String rendered = validRequest().toString();

        assertFalse(rendered.contains("M0123456"));
        assertFalse(rendered.contains("Ada Lovelace"));
        assertFalse(rendered.contains("v1:" + "a".repeat(64)));
        assertFalse(rendered.contains("AMP-ACC-000123"));
    }

    static CustomerVerificationRequest validRequest() {
        return new CustomerVerificationRequest(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                "M0123456",
                "Ada Lovelace",
                "AMPLITUDE",
                "v1:" + "a".repeat(64),
                "AMP-ACC-000123",
                "corr-4.4.3",
                UUID.fromString(
                        "c74e165f-df46-463e-a520-188e6df3e5ae"
                ),
                Instant.parse("2026-08-03T18:30:00Z")
        );
    }
}
