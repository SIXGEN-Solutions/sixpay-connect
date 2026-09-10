package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.model.financial.FinancialEntryDirection;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEntrySnapshot;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes =
                PaymentFinancialSnapshotPersistenceIT
                        .TestApplication.class,
        webEnvironment =
                SpringBootTest.WebEnvironment.NONE
)
@Testcontainers(disabledWithoutDocker = true)
class PaymentFinancialSnapshotPersistenceIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
            );
    private static final Instant T0 =
            Instant.parse("2026-09-06T18:00:00Z");

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
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );
    }

    @Autowired
    private PaymentFinancialSnapshotRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void preparePayment() {
        jdbcTemplate.update(
                "DELETE FROM payment_financial_entry_snapshots"
        );
        jdbcTemplate.update(
                "DELETE FROM payment_financial_event_snapshots"
        );
        jdbcTemplate.update(
                "DELETE FROM payments WHERE payment_id = ?",
                PAYMENT_ID
        );

        jdbcTemplate.update(
                "INSERT INTO payments ("
                        + "payment_id,"
                        + "public_payment_reference,"
                        + "payment_source,"
                        + "external_payment_reference,"
                        + "external_subscription_reference,"
                        + "financial_institution_code,"
                        + "requested_amount,"
                        + "requested_currency,"
                        + "status,"
                        + "business_version,"
                        + "received_at,"
                        + "updated_at,"
                        + "finalized_at,"
                        + "state_payload,"
                        + "persistence_version"
                        + ") VALUES ("
                        + "?,"
                        + "'PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV',"
                        + "'TRESOR_PAY',"
                        + "'TP-LOT24-001',"
                        + "'SUB-LOT24-001',"
                        + "'LRB',"
                        + "1000.00,"
                        + "'XAF',"
                        + "'RECEIVED',"
                        + "1,"
                        + "?,"
                        + "?,"
                        + "NULL,"
                        + "CAST('{\"schemaVersion\":1}' AS jsonb),"
                        + "0"
                        + ")",
                PAYMENT_ID,
                OffsetDateTime.ofInstant(
                        T0,
                        ZoneOffset.UTC
                ),
                OffsetDateTime.ofInstant(
                        T0,
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void persistsAndReloadsFinalizedSnapshotWithEntries() {
        PaymentFinancialEventSnapshot snapshot =
                finalizedSnapshot(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                );

        PaymentFinancialEventSnapshot persisted =
                repository.save(snapshot);

        assertEquals(
                snapshot.snapshotId(),
                persisted.snapshotId()
        );
        assertEquals(
                2,
                persisted.entries().size()
        );

        var reloaded =
                repository.findByPaymentId(
                        new PaymentId(PAYMENT_ID)
                );

        assertTrue(reloaded.isPresent());
        assertTrue(
                reloaded.orElseThrow()
                        .sameSnapshot(snapshot)
        );
    }

    @Test
    void refusesDraftPersistence() {
        assertThrows(
                PaymentPersistenceException.class,
                () -> repository.save(
                        draft(
                                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                        )
                )
        );
    }

    @Test
    void refusesDifferentSecondSnapshotForSamePayment() {
        repository.save(
                finalizedSnapshot(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                )
        );

        assertThrows(
                PaymentPersistenceException.class,
                () -> repository.save(
                        finalizedSnapshot(
                                "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
                        )
                )
        );
    }

    private static PaymentFinancialEventSnapshot
    finalizedSnapshot(String snapshotId) {
        PaymentFinancialEventSnapshot snapshot =
                draft(snapshotId);

        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        1,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref",
                        Money.of(
                                new BigDecimal("1000.00"),
                                "XAF"
                        ),
                        T0
                )
        );
        snapshot.addEntry(
                new PaymentFinancialEntrySnapshot(
                        UUID.fromString(
                                "22222222-2222-4222-8222-222222222222"
                        ),
                        2,
                        FinancialEntryDirection.CREDIT,
                        "creditor-ref",
                        Money.of(
                                new BigDecimal("1000.00"),
                                "XAF"
                        ),
                        T0
                )
        );
        snapshot.finalizeAt(
                T0.plusSeconds(1)
        );

        return snapshot;
    }

    private static PaymentFinancialEventSnapshot draft(
            String snapshotId
    ) {
        return PaymentFinancialEventSnapshot.draft(
                UUID.fromString(snapshotId),
                new PaymentId(PAYMENT_ID),
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                FinancialInstitutionCode.of("LRB"),
                "debtor-ref",
                "creditor-ref",
                Money.of(
                        new BigDecimal("1000.00"),
                        "XAF"
                ),
                "v1",
                T0
        );
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @EntityScan(
            basePackageClasses = {
                    PaymentFinancialEventSnapshotJpaEntity.class
            }
    )
    @EnableJpaRepositories(
            basePackageClasses = {
                    PaymentFinancialEventSnapshotSpringDataRepository.class
            }
    )
    static class TestApplication {

        @Bean
        PaymentFinancialSnapshotPersistenceMapper
        paymentFinancialSnapshotPersistenceMapper() {
            return new PaymentFinancialSnapshotPersistenceMapper();
        }

        @Bean
        PaymentFinancialSnapshotRepository
        paymentFinancialSnapshotRepository(
                PaymentFinancialEventSnapshotSpringDataRepository
                        repository,
                PaymentFinancialSnapshotPersistenceMapper mapper
        ) {
            return new PaymentFinancialSnapshotPersistenceAdapter(
                    repository,
                    mapper
            );
        }
    }
}
