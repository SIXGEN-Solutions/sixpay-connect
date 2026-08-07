package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.DailyAccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.VerifiedTresorPayStatusEligibilityPolicy;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AccountingBatchConstitutionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-07T23:10:00Z");

    @Test
    void persistsOnlyUnassignedCandidates() {
        AccountingPaymentCandidate first =
                candidate(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a",
                        "PAY-1"
                );
        AccountingPaymentCandidate second =
                candidate(
                        "43d7e460-4ca7-4ed1-8603-9f11fb62dd65",
                        "PAY-2"
                );

        FakeRepository repository =
                new FakeRepository();
        repository.assigned.add(
                first.paymentId()
        );

        AccountingBatchConstitutionService service =
                service(
                        window -> List.of(
                                first,
                                second
                        ),
                        repository
                );

        AccountingBatch batch = service.constitute(
                NOW,
                AccountingCutoffMode.MANUAL,
                Optional.of(
                        LocalDate.of(2026, 8, 7)
                ),
                "LAREGIONALE"
        );

        assertEquals(1, batch.items().size());
        assertEquals(
                second.paymentId(),
                batch.items()
                        .getFirst()
                        .paymentId()
        );
        assertEquals(1, repository.saved.size());
    }

    @Test
    void returnsExistingBatchForSameIdempotencyKey() {
        AccountingPaymentCandidate first =
                candidate(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a",
                        "PAY-1"
                );

        FakeRepository repository =
                new FakeRepository();

        AccountingBatchConstitutionService service =
                service(
                        window -> List.of(first),
                        repository
                );

        AccountingBatch created = service.constitute(
                NOW,
                AccountingCutoffMode.MANUAL,
                Optional.of(
                        LocalDate.of(2026, 8, 7)
                ),
                "LAREGIONALE"
        );

        repository.assigned.clear();

        AccountingBatch same = service.constitute(
                NOW,
                AccountingCutoffMode.MANUAL,
                Optional.of(
                        LocalDate.of(2026, 8, 7)
                ),
                "LAREGIONALE"
        );

        assertSame(created, same);
        assertEquals(1, repository.saved.size());
    }

    private static AccountingBatchConstitutionService service(
            PaymentAccountingCandidateSource source,
            AccountingBatchRepository repository
    ) {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        return new AccountingBatchConstitutionService(
                new DailyAccountingCutoffPolicy(
                        ZoneId.of("Africa/Douala"),
                        LocalTime.of(23, 0)
                ),
                source,
                new AccountingBatchBuilder(
                        new VerifiedTresorPayStatusEligibilityPolicy(),
                        new AccountingBatchIdempotencyKeyFactory(),
                        clock
                ),
                repository
        );
    }

    private static AccountingPaymentCandidate candidate(
            String id,
            String reference
    ) {
        return new AccountingPaymentCandidate(
                UUID.fromString(id),
                reference,
                "TRESORPAY",
                "LAREGIONALE",
                new BigDecimal("10000"),
                Currency.getInstance("XAF"),
                Instant.parse("2026-08-07T12:00:00Z"),
                LocalDate.of(2026, 8, 7),
                "AMP-" + reference,
                new TresorPayPaymentStatusEvidence(
                        "CONFIRMED",
                        Instant.parse("2026-08-07T12:05:00Z"),
                        "STATUS-" + reference,
                        "corr-" + reference
                )
        );
    }

    private static final class FakeRepository
            implements AccountingBatchRepository {

        private final Map<String, AccountingBatch> byKey =
                new LinkedHashMap<>();

        private final Map<UUID, AccountingBatch> byId =
                new LinkedHashMap<>();

        private final java.util.Set<UUID> assigned =
                new java.util.HashSet<>();

        private final java.util.List<AccountingBatch> saved =
                new java.util.ArrayList<>();

        @Override
        public AccountingBatch save(
                AccountingBatch batch
        ) {
            saved.add(batch);
            byId.put(
                    batch.batchId().value(),
                    batch
            );
            byKey.put(
                    batch.idempotencyKey().value(),
                    batch
            );
            batch.items().forEach(item ->
                    assigned.add(
                            item.paymentId()
                    )
            );
            return batch;
        }

        @Override
        public Optional<AccountingBatch> findById(
                AccountingBatchId batchId
        ) {
            return Optional.ofNullable(
                    byId.get(batchId.value())
            );
        }

        @Override
        public Optional<AccountingBatch>
        findByIdempotencyKey(
                AccountingBatchIdempotencyKey key
        ) {
            return Optional.ofNullable(
                    byKey.get(key.value())
            );
        }

        @Override
        public Set<UUID> findAssignedPaymentIds(
                Set<UUID> paymentIds
        ) {
            return paymentIds.stream()
                    .filter(assigned::contains)
                    .collect(
                            java.util.stream.Collectors
                                    .toUnmodifiableSet()
                    );
        }
    }
}
