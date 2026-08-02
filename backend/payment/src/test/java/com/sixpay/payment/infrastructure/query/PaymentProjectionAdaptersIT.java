package com.sixpay.payment.infrastructure.query;

import com.sixpay.payment.application.query.PaymentSearchSort;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.application.security.PaymentVisibilityScope;
import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PaymentProjectionAdaptersIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class PaymentProjectionAdaptersIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            );

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentProjectionReadAdapter readAdapter;

    @Autowired
    private PaymentObjectAccessAdapter accessAdapter;

    @Test
    void searchesDetailsAndPaginatesWithoutLoadingAggregate() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        insertPayment(
                first,
                "PAY-00000000000000000000000001",
                "TP-REQ-001",
                Instant.parse("2026-08-01T10:00:00Z")
        );
        insertPayment(
                second,
                "PAY-00000000000000000000000002",
                "TP-REQ-002",
                Instant.parse("2026-08-01T11:00:00Z")
        );

        var firstPage = readAdapter.search(
                query(null, 1),
                new PaymentVisibilityScope.Internal()
        );

        assertThat(firstPage.items()).hasSize(1);
        assertThat(firstPage.hasMore()).isTrue();
        assertThat(firstPage.nextCursor()).isNotBlank();
        assertThat(firstPage.items().getFirst()
                .tresorPayRequestId())
                .isEqualTo("TP-REQ-002");

        var secondPage = readAdapter.search(
                query(firstPage.nextCursor(), 1),
                new PaymentVisibilityScope.Internal()
        );

        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.hasMore()).isFalse();
        assertThat(secondPage.items().getFirst()
                .tresorPayRequestId())
                .isEqualTo("TP-REQ-001");

        var detail = readAdapter.findById(
                new PaymentId(first)
        ).orElseThrow();

        assertThat(detail.summary().debtorAccount()
                .maskedValue())
                .isEqualTo("MASKED-1234");
        assertThat(detail.bankingVerification()
                .outcome())
                .isEqualTo("VERIFIED");
        assertThat(detail.posting().outcome())
                .isEqualTo("CUT_CREDIT_CONFIRMED");
        assertThat(detail.tfj().status())
                .isEqualTo("INTEGRATED");
        assertThat(detail.notifications()).isEmpty();

        var descriptor = accessAdapter
                .findAccessDescriptor(
                        new PaymentId(first)
                )
                .orElseThrow();

        assertThat(descriptor.partnerSubjectOptional())
                .isEmpty();
    }

    @Test
    void partnerSearchIsFailClosed() {
        var page = readAdapter.search(
                query(null, 50),
                new PaymentVisibilityScope.Partner(
                        "partner-subject"
                )
        );

        assertThat(page.items()).isEmpty();
        assertThat(page.hasMore()).isFalse();
    }

    private SearchPaymentProjectionsQuery query(
            String cursor,
            int size
    ) {
        return new SearchPaymentProjectionsQuery(
                cursor,
                size,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PaymentSearchSort.CREATED_AT_DESC
        );
    }

    private void insertPayment(
            UUID paymentId,
            String publicReference,
            String externalReference,
            Instant receivedAt
    ) {
        UUID correlationId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO payments (
                    payment_id,
                    public_payment_reference,
                    payment_source,
                    external_payment_reference,
                    external_subscription_reference,
                    financial_institution_code,
                    requested_amount,
                    requested_currency,
                    status,
                    business_version,
                    received_at,
                    updated_at,
                    finalized_at,
                    state_payload,
                    persistence_version
                ) VALUES (
                    ?,
                    ?,
                    'TRESOR_PAY',
                    ?,
                    ?,
                    'SIXPAY_BANK',
                    1000.00,
                    'XAF',
                    'TREASURY_INTEGRATED',
                    7,
                    ?,
                    ?,
                    ?,
                    ?::jsonb,
                    0
                )
                """,
                paymentId,
                publicReference,
                externalReference,
                "SUB-" + paymentId,
                receivedAt.atOffset(
                        java.time.ZoneOffset.UTC
                ),
                receivedAt.plusSeconds(5)
                        .atOffset(
                                java.time.ZoneOffset.UTC
                        ),
                receivedAt.plusSeconds(6)
                        .atOffset(
                                java.time.ZoneOffset.UTC
                        ),
                statePayload(correlationId)
        );
    }

    private String statePayload(UUID correlationId) {
        return """
                {
                  "schemaVersion": 1,
                  "requestIdentity": {
                    "correlationId": {
                      "value": "%s"
                    }
                  },
                  "debtorAccountReference": {
                    "bindingFingerprint":
                      "v1:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                    "maskedDisplay": "MASKED-1234"
                  },
                  "bankingVerificationEvidence": {
                    "verificationId": {
                      "value": "VERIFY-001"
                    },
                    "outcome": "VERIFIED",
                    "checks": [
                      {
                        "reasonCode": null
                      }
                    ],
                    "metadata": {
                      "observedAt":
                        "2026-08-01T10:00:01Z"
                    }
                  },
                  "bankPostingReference": {
                    "principalPostingReference":
                      "BANK-POST-001"
                  },
                  "postingOutcomeEvidence": {
                    "outcome": "COMPLETED",
                    "metadata": {
                      "observedAt":
                        "2026-08-01T10:00:02Z"
                    }
                  },
                  "endOfDayConfirmationEvidence": {
                    "tfjStatus": "INTEGRATED",
                    "businessDate": "2026-08-01",
                    "confirmedAt":
                      "2026-08-01T10:00:03Z"
                  },
                  "reversalEvidence": null,
                  "failure": null
                }
                """.formatted(correlationId);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(
            PaymentModuleConfiguration.class
    )
    static class TestApplication {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return () ->
                    Optional.<AuthenticatedUser>empty();
        }
    }
}
