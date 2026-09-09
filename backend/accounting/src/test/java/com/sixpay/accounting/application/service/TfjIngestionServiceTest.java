package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.TfjConfirmationConflictException;
import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TfjIngestionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-09-08T22:00:00Z");

    @Test
    void integratedUniqueMatchIsPersistedAndPublishesFinalityIntent() {
        FakeRepository repository = new FakeRepository();
        UUID paymentId = UUID.randomUUID();
        List<Object> events = new ArrayList<>();

        TfjIngestionService service =
                new TfjIngestionService(
                        repository,
                        (bank, date, payment, posting) ->
                                List.of(paymentId),
                        events::add
                );

        TfjIngestionResult result =
                service.ingest(
                        candidate(TfjStatus.INTEGRATED)
                );

        assertEquals(
                TfjReceiptStatus.ACCEPTED_FOR_MATCHING,
                result.receiptStatus()
        );
        assertEquals(
                TfjMatchStatus.MATCHED,
                repository.saved.matchStatus()
        );
        assertEquals(
                paymentId,
                repository.saved.matchedPaymentId()
        );
        assertEquals(1, events.size());
        assertInstanceOf(
                TfjFinalityReadyEvent.class,
                events.getFirst()
        );
    }

    @Test
    void unmatchedConfirmationIsQuarantined() {
        FakeRepository repository = new FakeRepository();
        List<Object> events = new ArrayList<>();

        TfjIngestionService service =
                new TfjIngestionService(
                        repository,
                        (bank, date, payment, posting) ->
                                List.of(),
                        events::add
                );

        TfjIngestionResult result =
                service.ingest(
                        candidate(TfjStatus.INTEGRATED)
                );

        assertEquals(
                TfjReceiptStatus.QUARANTINED,
                result.receiptStatus()
        );
        assertEquals(
                TfjMatchStatus.UNMATCHED,
                repository.saved.matchStatus()
        );
        assertTrue(events.isEmpty());
    }

    @Test
    void identicalReplayIsNoOpAndDifferentPayloadConflicts() {
        FakeRepository repository = new FakeRepository();

        TfjIngestionService service =
                new TfjIngestionService(
                        repository,
                        (bank, date, payment, posting) ->
                                List.of(UUID.randomUUID()),
                        event -> {
                        }
                );

        TfjConfirmation original =
                candidate(TfjStatus.INTEGRATED);
        service.ingest(original);

        assertEquals(
                TfjReceiptStatus.IDENTICAL_REPLAY,
                service.ingest(original).receiptStatus()
        );

        TfjConfirmation conflicting =
                new TfjConfirmation(
                        original.confirmationId(),
                        original.idempotencyKey(),
                        "pending",
                        original.financialInstitutionCode(),
                        original.businessDate(),
                        original.paymentReference(),
                        original.bankPostingReference(),
                        original.tfjBatchReference(),
                        TfjStatus.PENDING,
                        original.confirmedAt(),
                        null,
                        null,
                        null,
                        original.receivedAt(),
                        original.observationChannel(),
                        original.correlationId(),
                        TfjMatchStatus.UNMATCHED,
                        null,
                        null
                );

        assertThrows(
                TfjConfirmationConflictException.class,
                () -> service.ingest(conflicting)
        );
    }

    private static TfjConfirmation candidate(
            TfjStatus status
    ) {
        boolean failed = status == TfjStatus.FAILED;

        return new TfjConfirmation(
                UUID.fromString(
                        "11111111-1111-1111-1111-111111111111"
                ),
                "tfj-idempotency-001",
                "pending",
                "LAREGIONALE",
                LocalDate.of(2026, 9, 8),
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "AMP-T0-42",
                "TFJ-42",
                status,
                NOW.minusSeconds(60),
                failed ? "TFJ_FAILED" : null,
                failed
                        ? "Core Banking reported failed TFJ integration"
                        : null,
                failed
                        ? TfjRecoveryAction.REVERSAL_REQUIRED
                        : null,
                NOW,
                TfjObservationChannel.ASYNC_CALLBACK,
                "11111111-2222-3333-4444-555555555555",
                TfjMatchStatus.UNMATCHED,
                null,
                null
        );
    }

    private static final class FakeRepository
            implements TfjConfirmationRepository {

        private TfjConfirmation saved;

        @Override
        public Optional<TfjConfirmation>
        findByConfirmationId(UUID id) {
            return saved != null
                    && saved.confirmationId().equals(id)
                    ? Optional.of(saved)
                    : Optional.empty();
        }

        @Override
        public Optional<TfjConfirmation>
        findByIdempotencyKey(String key) {
            return saved != null
                    && saved.idempotencyKey().equals(key)
                    ? Optional.of(saved)
                    : Optional.empty();
        }

        @Override
        public TfjConfirmation save(
                TfjConfirmation confirmation
        ) {
            saved = confirmation;
            return confirmation;
        }

        @Override
        public List<TfjConfirmation>
        findPendingFinalityPublication(int limit) {
            return List.of();
        }
    }
}
