package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.*;
import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TfjFinalityPublicationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-09-09T10:00:00Z");

    @Test
    void successfulPublicationMarksConfirmationAsPublished() {
        FakeRepository repository = new FakeRepository(candidate());
        List<IntegrationEventEnvelope> published = new ArrayList<>();

        TfjFinalityPublicationService service =
                new TfjFinalityPublicationService(
                        published::add,
                        repository,
                        new ObjectMapper(),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        service.afterCommit(
                new TfjFinalityReadyEvent(repository.confirmation)
        );

        assertEquals(1, published.size());
        assertEquals(
                NOW,
                repository.confirmation.finalityPublishedAt()
        );
        assertEquals(
                TfjFinalityPublicationService.EVENT_TYPE,
                published.getFirst().eventType()
        );
    }

    @Test
    void failedPublicationLeavesConfirmationRecoverable() {
        FakeRepository repository = new FakeRepository(candidate());
        IntegrationEventTransport failingTransport =
                event -> {
                    throw new IllegalStateException("transport unavailable");
                };

        TfjFinalityPublicationService service =
                new TfjFinalityPublicationService(
                        failingTransport,
                        repository,
                        new ObjectMapper(),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.afterCommit(
                        new TfjFinalityReadyEvent(repository.confirmation)
                )
        );

        assertNull(repository.confirmation.finalityPublishedAt());
    }

    @Test
    void pendingRecoveryPublishesPreviouslyUnpublishedTerminalConfirmation() {
        FakeRepository repository = new FakeRepository(candidate());
        List<IntegrationEventEnvelope> published = new ArrayList<>();

        TfjFinalityPublicationService service =
                new TfjFinalityPublicationService(
                        published::add,
                        repository,
                        new ObjectMapper(),
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        assertEquals(1, service.publishPending(10));
        assertEquals(1, published.size());
        assertEquals(NOW, repository.confirmation.finalityPublishedAt());
    }

    private static TfjConfirmation candidate() {
        return new TfjConfirmation(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "t1-7-idempotency",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "LAREGIONALE",
                LocalDate.of(2026, 9, 9),
                "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV",
                "AMP-T0-777",
                "TFJ-777",
                TfjStatus.INTEGRATED,
                NOW.minusSeconds(60),
                null,
                null,
                null,
                NOW.minusSeconds(30),
                TfjObservationChannel.ASYNC_CALLBACK,
                "11111111-2222-3333-4444-555555555555",
                TfjMatchStatus.MATCHED,
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                null
        );
    }

    private static final class FakeRepository
            implements TfjConfirmationRepository {

        private TfjConfirmation confirmation;

        private FakeRepository(TfjConfirmation confirmation) {
            this.confirmation = confirmation;
        }

        @Override
        public Optional<TfjConfirmation> findByConfirmationId(UUID confirmationId) {
            return Optional.ofNullable(confirmation)
                    .filter(value -> value.confirmationId().equals(confirmationId));
        }

        @Override
        public Optional<TfjConfirmation> findByIdempotencyKey(String idempotencyKey) {
            return Optional.ofNullable(confirmation)
                    .filter(value -> value.idempotencyKey().equals(idempotencyKey));
        }

        @Override
        public TfjConfirmation save(TfjConfirmation value) {
            confirmation = value;
            return value;
        }

        @Override
        public TfjConfirmationRepository.OperationalPage searchOperational(
                java.time.LocalDate businessDate,
                String paymentReference,
                String bankPostingReference,
                int page,
                int size
        ) {
            List<TfjConfirmation> content =
                    confirmation == null
                            ? List.of()
                            : List.of(confirmation);
            return new TfjConfirmationRepository.OperationalPage(
                    content,
                    content.size()
            );
        }

        @Override
        public List<TfjConfirmation> searchOperationalAll(
                java.time.LocalDate businessDate,
                String paymentReference,
                String bankPostingReference
        ) {
            return confirmation == null
                    ? List.of()
                    : List.of(confirmation);
        }

        @Override
        public List<TfjConfirmation> findPendingFinalityPublication(int limit) {
            if (confirmation != null
                    && confirmation.terminal()
                    && confirmation.matchedPaymentId() != null
                    && confirmation.finalityPublishedAt() == null) {
                return List.of(confirmation);
            }
            return List.of();
        }
    }
}
