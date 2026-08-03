package com.sixpay.customer.verification.infrastructure.banking.mapper;

import com.sixpay.customer.verification.application.port.out.BankingVerificationResponse;
import com.sixpay.customer.verification.domain.model.VerificationCheckResult;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerVerificationResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmplitudeCustomerVerificationMapperTest {

    private final AmplitudeCustomerVerificationMapper mapper =
            new AmplitudeCustomerVerificationMapper(
                    Duration.ofMinutes(5)
            );

    @Test
    void mapsAllElevenChecksAndDefaultTtl() {
        Instant observedAt =
                Instant.parse("2026-08-03T16:00:00Z");

        BankingVerificationResponse response =
                mapper.toInternalResponse(
                        response(
                                observedAt,
                                allChecks("PASS")
                        )
                );

        assertEquals(11, response.checks().size());
        assertEquals(
                observedAt.plus(Duration.ofMinutes(5)),
                response.validUntil()
        );
    }

    @Test
    void mapsFailAndUnknownWithoutComputingGlobalOutcome() {
        Map<String, String> checks = allChecks("PASS");
        checks.put("ACCOUNT_EXISTS", "FAIL");
        checks.put("NIU_MATCHES", "UNKNOWN");

        BankingVerificationResponse response =
                mapper.toInternalResponse(
                        response(
                                Instant.parse(
                                        "2026-08-03T16:00:00Z"
                                ),
                                checks
                        )
                );

        assertEquals(
                VerificationCheckResult.FAIL,
                response.checks().stream()
                        .filter(
                                check -> check.type()
                                        == VerificationCheckType.ACCOUNT_EXISTS
                        )
                        .findFirst()
                        .orElseThrow()
                        .result()
        );
        assertEquals(
                VerificationCheckResult.UNKNOWN,
                response.checks().stream()
                        .filter(
                                check -> check.type()
                                        == VerificationCheckType.NIU_MATCHES
                        )
                        .findFirst()
                        .orElseThrow()
                        .result()
        );
    }

    private static AmplitudeCustomerVerificationResponse response(
            Instant observedAt,
            Map<String, String> checks
    ) {
        return new AmplitudeCustomerVerificationResponse(
                "200",
                true,
                "ACTIVE",
                "Ada Lovelace",
                "10005-*****-*******8901-12",
                "XAF",
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(100_000),
                true,
                "Account verified",
                "SUCCESS",
                observedAt,
                null,
                checks
        );
    }

    private static Map<String, String> allChecks(
            String result
    ) {
        return Arrays.stream(VerificationCheckType.values())
                .collect(
                        Collectors.toMap(
                                Enum::name,
                                ignored -> result
                        )
                );
    }
}
