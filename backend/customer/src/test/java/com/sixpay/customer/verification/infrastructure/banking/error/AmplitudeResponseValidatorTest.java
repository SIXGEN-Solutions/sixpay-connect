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


    @Test
    void acceptsEightCoreChecksWhenNeitherIdentityNorKycWasRequested() {
        var response = withChecks(response("VERIFIED"),
                checks("PASS").stream()
                        .filter(check -> !check.type().equals("IDENTITY_MATCHES"))
                        .filter(check -> !check.type().startsWith("REQUIRED_KYC_"))
                        .toList());
        assertThat(validator.validate(response, false, false)).isNotNull();
    }

    @Test
    void failsClosedWhenRequestedIdentityOrKycChecksAreAbsent() {
        var coreOnly = withChecks(response("VERIFIED"), checks("PASS").stream()
                .filter(check -> !check.type().equals("IDENTITY_MATCHES"))
                .filter(check -> !check.type().startsWith("REQUIRED_KYC_"))
                .toList());
        assertThatThrownBy(() -> validator.validate(coreOnly, true, false))
                .isInstanceOf(AmplitudeInvalidResponseException.class);
        assertThatThrownBy(() -> validator.validate(coreOnly, false, true))
                .isInstanceOf(AmplitudeInvalidResponseException.class);
        assertThatThrownBy(() -> validator.validate(withChecks(coreOnly,
                coreOnly.checks().subList(1, coreOnly.checks().size())), false, false))
                .isInstanceOf(AmplitudeInvalidResponseException.class);
    }

    @Test
    void rejectsFailedOrUnknownChecksEvenWhenOptional() {
        for (String bad : List.of("FAIL", "UNKNOWN")) {
            var all = new java.util.ArrayList<>(checks("PASS"));
            all.set(3, new AmplitudeVerificationCheckResponse("IDENTITY_MATCHES", bad, null,
                    Instant.parse("2026-08-30T20:01:00Z")));
            var invalid = withChecks(response("VERIFIED"), all);
            assertThatThrownBy(() -> validator.validate(invalid, false, false))
                    .isInstanceOf(AmplitudeInvalidResponseException.class);
        }
    }

    private static AmplitudeCustomerVerificationResponse withChecks(
            AmplitudeCustomerVerificationResponse r,
            List<AmplitudeVerificationCheckResponse> checks) {
        return new AmplitudeCustomerVerificationResponse(r.verificationId(), r.verifiedAt(),
                r.source(), r.outcome(), r.customerReference(), r.accountReference(),
                checks, r.identity(), r.account());
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
                "00001-0001-47",
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
                new BankAccount(
                        "0001", "00001", "001", null, "47", "CUST-0001",
                        null, null, null, null, null, "CURRENT", null, null, "ACTIVE"
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
