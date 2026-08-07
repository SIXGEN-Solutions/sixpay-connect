package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingSubmissionOutcomeUnknownException;
import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchItemStatus;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingProviderBatchResult;
import com.sixpay.accounting.domain.model.AccountingProviderItemResult;
import com.sixpay.accounting.domain.model.AccountingSubmissionState;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.accounting.domain.repository.AccountingReconciliationRepository;
import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountingBatchReconciliationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-07T20:00:00Z");

    @Test
    void unknownSubmissionIsNeverBlindlyResubmitted() {
        AccountingBatch batch = batch();
        FakePersistence persistence =
                new FakePersistence(batch);
        FakeGateway gateway = new FakeGateway();
        gateway.unknownOnSubmit = true;

        AccountingBatchReconciliationService service =
                service(
                        persistence,
                        gateway
                );

        assertThrows(
                AccountingSubmissionOutcomeUnknownException.class,
                () -> service.submitOrReconcile(
                        batch.batchId(),
                        context()
                )
        );

        assertEquals(
                AccountingSubmissionState.OUTCOME_UNKNOWN,
                persistence.tracking
                        .submissionState()
        );
        assertEquals(1, gateway.submitCalls.get());

        gateway.unknownOnSubmit = false;
        gateway.lookupResult = Optional.of(
                completedResult(batch)
        );

        AccountingBatchTracking resolved =
                service.submitOrReconcile(
                        batch.batchId(),
                        context()
                );

        assertEquals(1, gateway.submitCalls.get());
        assertEquals(
                AccountingSubmissionState.COMPLETED,
                resolved.submissionState()
        );
        assertEquals(
                AccountingBatchStatus.COMPLETED,
                persistence.batch.status()
        );
    }


    @Test
    void crashAfterSubmissionIntentNeverTriggersBlindPost() {
        AccountingBatch batch = batch();
        FakePersistence persistence =
                new FakePersistence(batch);
        persistence.tracking =
                AccountingBatchTracking.ready(
                                batch.batchId()
                        )
                        .submissionAttempted(NOW);

        FakeGateway gateway = new FakeGateway();

        AccountingBatchTracking result =
                service(
                        persistence,
                        gateway
                ).submitOrReconcile(
                        batch.batchId(),
                        context()
                );

        assertEquals(0, gateway.submitCalls.get());
        assertEquals(1, gateway.idempotencyLookups.get());
        assertEquals(1, gateway.batchLookups.get());
        assertEquals(
                AccountingSubmissionState.SUBMITTING,
                result.submissionState()
        );
    }

    @Test
    void reconciliationUsesIdempotencyLookupFirst() {
        AccountingBatch batch = batch();
        FakePersistence persistence =
                new FakePersistence(batch);
        persistence.tracking =
                AccountingBatchTracking.ready(
                                batch.batchId()
                        )
                        .outcomeUnknown(
                                NOW,
                                "SUBMISSION_OUTCOME_UNKNOWN"
                        );

        FakeGateway gateway = new FakeGateway();
        gateway.lookupResult = Optional.of(
                completedResult(batch)
        );

        AccountingBatchTracking result =
                service(
                        persistence,
                        gateway
                ).reconcile(
                        batch.batchId(),
                        context()
                );

        assertEquals(1, gateway.idempotencyLookups.get());
        assertEquals(0, gateway.batchLookups.get());
        assertEquals(
                AccountingSubmissionState.COMPLETED,
                result.submissionState()
        );
    }

    @Test
    void missingProviderResultKeepsUnknownState() {
        AccountingBatch batch = batch();
        FakePersistence persistence =
                new FakePersistence(batch);
        persistence.tracking =
                AccountingBatchTracking.ready(
                                batch.batchId()
                        )
                        .outcomeUnknown(
                                NOW,
                                "SUBMISSION_OUTCOME_UNKNOWN"
                        );

        FakeGateway gateway = new FakeGateway();

        AccountingBatchTracking result =
                service(
                        persistence,
                        gateway
                ).reconcile(
                        batch.batchId(),
                        context()
                );

        assertEquals(
                AccountingSubmissionState.OUTCOME_UNKNOWN,
                result.submissionState()
        );
        assertEquals(1, result.reconciliationAttempts());
        assertEquals(1, gateway.idempotencyLookups.get());
        assertEquals(1, gateway.batchLookups.get());
    }

    private static AccountingBatchReconciliationService
    service(
            FakePersistence persistence,
            FakeGateway gateway
    ) {
        return new AccountingBatchReconciliationService(
                persistence,
                persistence,
                persistence,
                gateway,
                Clock.fixed(
                        NOW,
                        ZoneOffset.UTC
                )
        );
    }

    private static AccountingIntegrationContext context() {
        return new AccountingIntegrationContext(
                CorrelationId.of(
                        "corr-accounting-5.6.4"
                ),
                UUID.fromString(
                        "bd1f1125-778d-43f7-b8bf-fdd93d3f5542"
                )
        );
    }

    private static AccountingBatch batch() {
        return new AccountingBatch(
                new AccountingBatchId(
                        UUID.fromString(
                                "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                        )
                ),
                new AccountingBatchIdempotencyKey(
                        "a".repeat(64)
                ),
                LocalDate.of(2026, 8, 7),
                "LAREGIONALE",
                Instant.parse(
                        "2026-08-07T19:00:00Z"
                ),
                AccountingBatchStatus.NOT_COMPLETED,
                List.of(
                        new AccountingBatchItem(
                                UUID.fromString(
                                        "43d7e460-4ca7-4ed1-8603-9f11fb62dd65"
                                ),
                                "PAY-20260807-0001",
                                "TRESORPAY",
                                new BigDecimal("10000"),
                                Currency.getInstance("XAF"),
                                Instant.parse(
                                        "2026-08-07T12:00:00Z"
                                ),
                                LocalDate.of(2026, 8, 7),
                                "AMP-POST-1",
                                "CONFIRMED",
                                Instant.parse(
                                        "2026-08-07T12:05:00Z"
                                ),
                                AccountingBatchItemStatus.PENDING
                        )
                )
        );
    }

    private static AccountingProviderBatchResult
    completedResult(
            AccountingBatch batch
    ) {
        return new AccountingProviderBatchResult(
                batch.batchId(),
                batch.idempotencyKey(),
                AccountingBatchStatus.COMPLETED,
                "ACC-PROVIDER-42",
                NOW,
                List.of(
                        new AccountingProviderItemResult(
                                batch.items()
                                        .getFirst()
                                        .paymentId(),
                                AccountingBatchItemStatus.COMPLETED,
                                "ACC-ITEM-42",
                                null
                        )
                )
        );
    }

    private static final class FakeGateway
            implements AccountingBatchGateway {

        private final AtomicInteger submitCalls =
                new AtomicInteger();

        private final AtomicInteger idempotencyLookups =
                new AtomicInteger();

        private final AtomicInteger batchLookups =
                new AtomicInteger();

        private boolean unknownOnSubmit;

        private Optional<AccountingProviderBatchResult>
                lookupResult = Optional.empty();

        @Override
        public AccountingProviderBatchResult submit(
                AccountingBatch batch,
                AccountingIntegrationContext context
        ) {
            submitCalls.incrementAndGet();

            if (unknownOnSubmit) {
                throw new AccountingSubmissionOutcomeUnknownException(
                        "unknown",
                        null
                );
            }

            return completedResult(batch);
        }

        @Override
        public Optional<AccountingProviderBatchResult>
        findByBatchId(
                AccountingBatchId batchId,
                AccountingIntegrationContext context
        ) {
            batchLookups.incrementAndGet();
            return lookupResult;
        }

        @Override
        public Optional<AccountingProviderBatchResult>
        findByIdempotencyKey(
                AccountingBatchIdempotencyKey idempotencyKey,
                AccountingIntegrationContext context
        ) {
            idempotencyLookups.incrementAndGet();
            return lookupResult;
        }
    }

    private static final class FakePersistence
            implements AccountingBatchRepository,
            AccountingBatchTrackingRepository,
            AccountingReconciliationRepository {

        private AccountingBatch batch;
        private AccountingBatchTracking tracking;

        private FakePersistence(
                AccountingBatch batch
        ) {
            this.batch = batch;
        }

        @Override
        public AccountingBatch save(
                AccountingBatch batch
        ) {
            this.batch = batch;
            return batch;
        }

        @Override
        public Optional<AccountingBatch> findById(
                AccountingBatchId batchId
        ) {
            return batch.batchId().equals(batchId)
                    ? Optional.of(batch)
                    : Optional.empty();
        }

        @Override
        public Optional<AccountingBatch>
        findByIdempotencyKey(
                AccountingBatchIdempotencyKey key
        ) {
            return batch.idempotencyKey().equals(key)
                    ? Optional.of(batch)
                    : Optional.empty();
        }

        @Override
        public Set<UUID> findAssignedPaymentIds(
                Set<UUID> paymentIds
        ) {
            return Set.of();
        }

        @Override
        public AccountingBatchTracking save(
                AccountingBatchTracking tracking
        ) {
            this.tracking = tracking;
            return tracking;
        }

        @Override
        public Optional<AccountingBatchTracking>
        findByBatchId(
                AccountingBatchId batchId
        ) {
            if (tracking == null) {
                return Optional.empty();
            }

            return tracking.batchId().equals(batchId)
                    ? Optional.of(tracking)
                    : Optional.empty();
        }

        @Override
        public AccountingBatchTracking saveTracking(
                AccountingBatchTracking tracking
        ) {
            return save(
                    tracking
            );
        }

        @Override
        public AccountingBatchTracking saveResult(
                AccountingBatch batch,
                AccountingBatchTracking tracking
        ) {
            this.batch = batch;

            return save(
                    tracking
            );
        }
    }
}
