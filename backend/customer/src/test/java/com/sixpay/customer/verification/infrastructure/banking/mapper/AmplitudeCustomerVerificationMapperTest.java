package com.sixpay.customer.verification.infrastructure.banking.mapper;

import com.sixpay.customer.verification.application.port.output.*;
import com.sixpay.customer.verification.domain.model.*;
import com.sixpay.customer.verification.infrastructure.banking.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AmplitudeCustomerVerificationMapperTest {

    private final AmplitudeCustomerVerificationMapper mapper =
            new AmplitudeCustomerVerificationMapper(
                    Duration.ofMinutes(5)
            );

    @Test
    void mapsApprovedRequestShape() {
        Instant requestedAt =
                Instant.parse("2026-08-30T20:00:00Z");

        BankingVerificationQuery query =
                new BankingVerificationQuery(
                        new CustomerVerificationId(
                                UUID.fromString(
                                        "2c65ae19-c8a7-4db7-a617-0769f3c9e7ec"
                                )
                        ),
                        CustomerVerificationSubject.of(
                                CustomerIdentity.of(
                                        CustomerNiu.of("M012345678901"),
                                        "Ada Lovelace"
                                )
                        ),
                        FinancialInstitutionCode.of("AMPLITUDE"),
                        AccountBindingFingerprint.of(
                                "v1:"
                                        + "a".repeat(64)
                        ),
                        BankingAccountAccessReference.of(
                                "ACCOUNT-CANONICAL-001"
                        ),
                        CustomerVerificationContext.of(
                                com.sixpay.common.context.CorrelationId.of(
                                        "f7316f4e-bb10-43b0-a78c-663a3eb79df3"
                                ),
                                null
                        ),
                        requestedAt
                );

        AmplitudeCustomerVerificationRequest request =
                mapper.toExternalRequest(query);

        assertThat(request.financialInstitutionCode())
                .isEqualTo("AMPLITUDE");
        assertThat(request.customer().niu())
                .isEqualTo("M012345678901");
        assertThat(request.customer().legalName())
                .isEqualTo("Ada Lovelace");
        assertThat(request.account().accountReference())
                .isEqualTo("ACCOUNT-CANONICAL-001");
        assertThat(request.requiredKycFields())
                .containsExactly(
                        "niu",
                        "legalName",
                        "phoneNumber",
                        "email"
                );
        assertThat(request.requestedAt()).isEqualTo(requestedAt);
    }

    @Test
    void exposesCanonicalReferencesAndKycContactEvidence() {
        Instant verifiedAt =
                Instant.parse("2026-08-30T20:01:00Z");

        BankingVerificationResponse response =
                mapper.toInternalResponse(
                        approvedResponse(verifiedAt)
                );

        assertThat(response.checks()).hasSize(11);
        assertThat(response.customerReference())
                .isEqualTo("CUST-0001");
        assertThat(response.accountReference())
                .isEqualTo("ACC-0001");
        assertThat(response.identity().phoneNumber())
                .isEqualTo("+237690000001");
        assertThat(response.identity().email())
                .isEqualTo("ada@example.test");
        assertThat(response.identity().kycStatus())
                .isEqualTo("COMPLETE");
        assertThat(response.identity().kycFields())
                .allMatch(
                        field -> field.present()
                                && field.verified()
                );
        assertThat(response.account().accountReference())
                .isEqualTo("ACC-0001");
        assertThat(response.validUntil())
                .isEqualTo(verifiedAt.plus(Duration.ofMinutes(5)));
    }

    private static AmplitudeCustomerVerificationResponse approvedResponse(
            Instant verifiedAt
    ) {
        List<AmplitudeVerificationCheckResponse> checks =
                Arrays.stream(VerificationCheckType.values())
                        .map(type ->
                                new AmplitudeVerificationCheckResponse(
                                        type.name(),
                                        "PASS",
                                        null,
                                        verifiedAt
                                )
                        )
                        .toList();

        List<AmplitudeKycFieldResponse> kycFields = List.of(
                new AmplitudeKycFieldResponse(
                        "niu",
                        null,
                        true,
                        true,
                        verifiedAt
                ),
                new AmplitudeKycFieldResponse(
                        "legalName",
                        null,
                        true,
                        true,
                        verifiedAt
                ),
                new AmplitudeKycFieldResponse(
                        "phoneNumber",
                        null,
                        true,
                        true,
                        verifiedAt
                ),
                new AmplitudeKycFieldResponse(
                        "email",
                        null,
                        true,
                        true,
                        verifiedAt
                )
        );

        return new AmplitudeCustomerVerificationResponse(
                UUID.fromString(
                        "2c65ae19-c8a7-4db7-a617-0769f3c9e7ec"
                ),
                verifiedAt,
                "AMPLITUDE",
                "VERIFIED",
                "CUST-0001",
                "ACC-0001",
                checks,
                new AmplitudeCustomerIdentityResponse(
                        "CUST-0001",
                        "000001",
                        "AMPLITUDE",
                        "M012345678901",
                        "Ada Lovelace",
                        "+237690000001",
                        "ada@example.test",
                        "COMPLETE",
                        kycFields,
                        verifiedAt,
                        "AMPLITUDE",
                        verifiedAt
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
                        verifiedAt
                )
        );
    }
}
