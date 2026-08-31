package com.sixpay.customer.verification.infrastructure.banking.error;

import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.infrastructure.banking.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AmplitudeResponseValidatorTest {

    private final AmplitudeResponseValidator validator =
            new AmplitudeResponseValidator();

    @Test
    void acceptsVerifiedContractResponse() {
        assertThat(
                validator.validate(response("VERIFIED"))
        ).isNotNull();
    }

    @Test
    void acceptsConclusiveNegativeWithoutBankingIdentity() {
        AmplitudeCustomerVerificationResponse response =
                new AmplitudeCustomerVerificationResponse(
                        UUID.randomUUID(),
                        Instant.parse("2026-08-30T20:01:00Z"),
                        "AMPLITUDE",
                        "REJECTED",
                        null,
                        null,
                        checks("FAIL"),
                        null,
                        null
                );

        assertThat(validator.validate(response))
                .isNotNull();
    }

    @Test
    void rejectsVerifiedResponseWithoutCanonicalReferences() {
        AmplitudeCustomerVerificationResponse response =
                new AmplitudeCustomerVerificationResponse(
                        UUID.randomUUID(),
                        Instant.parse("2026-08-30T20:01:00Z"),
                        "AMPLITUDE",
                        "VERIFIED",
                        null,
                        null,
                        checks("PASS"),
                        null,
                        null
                );

        assertThatThrownBy(() -> validator.validate(response))
                .isInstanceOf(
                        AmplitudeInvalidResponseException.class
                );
    }

    private static AmplitudeCustomerVerificationResponse response(
            String outcome
    ) {
        Instant instant =
                Instant.parse("2026-08-30T20:01:00Z");

        return new AmplitudeCustomerVerificationResponse(
                UUID.randomUUID(),
                instant,
                "AMPLITUDE",
                outcome,
                "CUST-0001",
                "ACC-0001",
                checks("PASS"),
                new AmplitudeCustomerIdentityResponse(
                        "CUST-0001",
                        "000001",
                        "AMPLITUDE",
                        "M012345678901",
                        "Ada Lovelace",
                        "+237690000001",
                        "ada@example.test",
                        "COMPLETE",
                        List.of(),
                        instant,
                        "AMPLITUDE",
                        instant
                ),
                new AmplitudeBankAccountResponse(
                        "ACC-0001",
                        "CUST-0001",
                        "AMPLITUDE",
                        "****0001",
                        "XAF",
                        "CURRENT",
                        "ACTIVE",
                        List.of(),
                        "AMPLITUDE",
                        instant
                )
        );
    }

    private static List<AmplitudeVerificationCheckResponse> checks(
            String result
    ) {
        return Arrays.stream(VerificationCheckType.values())
                .map(type ->
                        new AmplitudeVerificationCheckResponse(
                                type.name(),
                                result,
                                null,
                                Instant.parse(
                                        "2026-08-30T20:01:00Z"
                                )
                        )
                )
                .toList();
    }
}
