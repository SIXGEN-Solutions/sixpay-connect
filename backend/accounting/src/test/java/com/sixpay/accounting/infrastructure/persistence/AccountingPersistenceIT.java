package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.configuration.AccountingModuleConfiguration;
import com.sixpay.accounting.domain.exception
        .AccountingBatchPersistenceConflictException;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.domain.repository
        .AccountingBatchTrackingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = AccountingPersistenceIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Testcontainers
class AccountingPersistenceIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse(
                            "postgres:15-alpine"
                    )
            );

    private static final Instant CREATED_AT =
            Instant.parse(
                    "2026-08-09T16:30:00Z"
            );

    private static final LocalDate BUSINESS_DATE =
            LocalDate.of(
                    2026,
                    8,
                    9
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
    private AccountingBatchRepository batchRepository;

    @Autowired
    private AccountingBatchTrackingRepository trackingRepository;

    @Autowired
    private AccountingBatchSpringDataRepository
            springDataRepository;

    @Test
    void persistsAndReloadsBatchFromPostgreSql() {
        AccountingBatch batch =
                batch(
                        "11111111-1111-4111-8111-111111111111",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        "21111111-1111-4111-8111-111111111111",
                        "PAY-ACC-001"
                );

        AccountingBatch saved =
                batchRepository.save(
                        batch
                );

        AccountingBatch reloaded =
                batchRepository
                        .findById(
                                batch.batchId()
                        )
                        .orElseThrow();

        assertThat(
                saved.batchId()
        ).isEqualTo(
                reloaded.batchId()
        );

        assertThat(
                saved.idempotencyKey()
        ).isEqualTo(
                reloaded.idempotencyKey()
        );

        assertThat(
                saved.businessDate()
        ).isEqualTo(
                reloaded.businessDate()
        );

        assertThat(
                saved.financialInstitutionCode()
        ).isEqualTo(
                reloaded.financialInstitutionCode()
        );

        assertThat(
                saved.createdAt()
        ).isEqualTo(
                reloaded.createdAt()
        );

        assertThat(
                saved.status()
        ).isEqualTo(
                reloaded.status()
        );

        assertThat(
                reloaded.items()
        ).hasSize(1);

        AccountingBatchItem savedItem =
                saved.items()
                        .getFirst();

        AccountingBatchItem reloadedItem =
                reloaded.items()
                        .getFirst();

        assertThat(
                savedItem.paymentId()
        ).isEqualTo(
                reloadedItem.paymentId()
        );

        assertThat(
                savedItem.publicPaymentReference()
        ).isEqualTo(
                reloadedItem.publicPaymentReference()
        );

        assertThat(
                savedItem.partnerId()
        ).isEqualTo(
                reloadedItem.partnerId()
        );

        assertThat(
                savedItem.amount()
        ).isEqualByComparingTo(
                reloadedItem.amount()
        );

        assertThat(
                savedItem.currency()
        ).isEqualTo(
                reloadedItem.currency()
        );

        assertThat(
                savedItem.paymentOccurredAt()
        ).isEqualTo(
                reloadedItem.paymentOccurredAt()
        );

        assertThat(
                savedItem.paymentBusinessDate()
        ).isEqualTo(
                reloadedItem.paymentBusinessDate()
        );

        assertThat(
                savedItem.bankPostingReference()
        ).isEqualTo(
                reloadedItem.bankPostingReference()
        );

        assertThat(
                savedItem.tresorPayStatus()
        ).isEqualTo(
                reloadedItem.tresorPayStatus()
        );

        assertThat(
                savedItem.tresorPayStatusCheckedAt()
        ).isEqualTo(
                reloadedItem.tresorPayStatusCheckedAt()
        );

        assertThat(
                savedItem.status()
        ).isEqualTo(
                reloadedItem.status()
        );

        assertThat(
                batchRepository
                        .findByIdempotencyKey(
                                batch.idempotencyKey()
                        )
        ).contains(
                reloaded
        );

        assertThat(
                batchRepository
                        .findAssignedPaymentIds(
                                Set.of(
                                        batch.items()
                                                .getFirst()
                                                .paymentId()
                                )
                        )
        ).containsExactly(
                batch.items()
                        .getFirst()
                        .paymentId()
        );
    }

    @Test
    void searchesAccountingBatchesAgainstPostgreSql() {
        AccountingBatch batch =
                batch(
                        "17777777-7777-4777-8777-777777777777",
                        "ffffffffffffffffffffffffffffffff"
                                + "ffffffffffffffffffffffffffffffff",
                        "27777777-7777-4777-8777-777777777777",
                        "PAY-ACC-007"
                );

        batchRepository.save(
                batch
        );

        var result =
                springDataRepository
                        .findAllByBusinessDateAndStatus(
                                BUSINESS_DATE,
                                AccountingBatchStatus
                                        .NOT_COMPLETED,
                                PageRequest.of(
                                        0,
                                        20,
                                        Sort.by(
                                                Sort.Direction.DESC,
                                                "createdAt"
                                        )
                                )
                        );

        assertThat(
                result.getTotalElements()
        ).isEqualTo(
                1
        );

        assertThat(
                result.getContent()
        )
                .singleElement()
                .satisfies(
                        entity -> {
                            assertThat(
                                    entity.id()
                            ).isEqualTo(
                                    batch.batchId()
                                            .value()
                            );

                            assertThat(
                                    entity.status()
                            ).isEqualTo(
                                    AccountingBatchStatus
                                            .NOT_COMPLETED
                            );

                            assertThat(
                                    entity.businessDate()
                            ).isEqualTo(
                                    BUSINESS_DATE
                            );
                        }
                );
    }

    @Test
    void persistsAndReloadsTrackingFromPostgreSql() {
        AccountingBatch batch =
                batch(
                        "12222222-2222-4222-8222-222222222222",
                        "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                                + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                        "22222222-2222-4222-8222-222222222222",
                        "PAY-ACC-002"
                );

        batchRepository.save(
                batch
        );

        AccountingBatchTracking tracking =
                AccountingBatchTracking
                        .ready(
                                batch.batchId()
                        )
                        .submissionAttempted(
                                CREATED_AT
                        )
                        .outcomeUnknown(
                                CREATED_AT.plusSeconds(
                                        5
                                ),
                                "ACCOUNTING_TIMEOUT"
                        );

        trackingRepository.save(
                tracking
        );

        AccountingBatchTracking reloaded =
                trackingRepository
                        .findByBatchId(
                                batch.batchId()
                        )
                        .orElseThrow();

        assertThat(
                reloaded
        ).isEqualTo(
                tracking
        );
    }

    @Test
    void rejectsDuplicateIdempotencyKey() {
        AccountingBatch first =
                batch(
                        "13333333-3333-4333-8333-333333333333",
                        "cccccccccccccccccccccccccccccccc"
                                + "cccccccccccccccccccccccccccccccc",
                        "23333333-3333-4333-8333-333333333333",
                        "PAY-ACC-003"
                );

        AccountingBatch second =
                batch(
                        "14444444-4444-4444-8444-444444444444",
                        "cccccccccccccccccccccccccccccccc"
                                + "cccccccccccccccccccccccccccccccc",
                        "24444444-4444-4444-8444-444444444444",
                        "PAY-ACC-004"
                );

        batchRepository.save(
                first
        );

        assertThatThrownBy(
                () ->
                        batchRepository.save(
                                second
                        )
        ).isInstanceOf(
                AccountingBatchPersistenceConflictException.class
        );
    }

    @Test
    void rejectsPaymentAssignedToAnotherBatch() {
        UUID duplicatedPaymentId =
                UUID.fromString(
                        "25555555-5555-4555-8555-555555555555"
                );

        AccountingBatch first =
                batch(
                        "15555555-5555-4555-8555-555555555555",
                        "dddddddddddddddddddddddddddddddd"
                                + "dddddddddddddddddddddddddddddddd",
                        duplicatedPaymentId.toString(),
                        "PAY-ACC-005"
                );

        AccountingBatch second =
                batch(
                        "16666666-6666-4666-8666-666666666666",
                        "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
                                + "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                        duplicatedPaymentId.toString(),
                        "PAY-ACC-006"
                );

        batchRepository.save(
                first
        );

        assertThatThrownBy(
                () ->
                        batchRepository.save(
                                second
                        )
        ).isInstanceOf(
                AccountingBatchPersistenceConflictException.class
        );
    }

    private static AccountingBatch batch(
            String batchId,
            String idempotencyKey,
            String paymentId,
            String paymentReference
    ) {
        return new AccountingBatch(
                new AccountingBatchId(
                        UUID.fromString(
                                batchId
                        )
                ),
                new AccountingBatchIdempotencyKey(
                        idempotencyKey
                ),
                BUSINESS_DATE,
                "LAREGIONALE",
                CREATED_AT,
                AccountingBatchStatus.NOT_COMPLETED,
                List.of(
                        new AccountingBatchItem(
                                UUID.fromString(
                                        paymentId
                                ),
                                paymentReference,
                                "TRESORPAY",
                                new BigDecimal(
                                        "12500.00"
                                ),
                                Currency.getInstance(
                                        "XAF"
                                ),
                                CREATED_AT.minusSeconds(
                                        60
                                ),
                                BUSINESS_DATE,
                                "BANK-POST-001",
                                "SUCCESS",
                                CREATED_AT.minusSeconds(
                                        30
                                ),
                                AccountingBatchItemStatus.PENDING
                        )
                )
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ImportAutoConfiguration(
            AccountingModuleConfiguration.class
    )
    static class TestApplication {
    }
}