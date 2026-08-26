package com.sixpay.payment.infrastructure.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentIdempotencyReplayStoreTest {

    private static final String OPERATION =
            "PAYMENT_CREATE";
    private static final String KEY =
            "idem-001";
    private static final String HASH =
            "a".repeat(64);
    private static final Instant NOW =
            Instant.parse("2026-08-01T19:00:00Z");

    private PaymentIdempotencyRepository repository;
    private PaymentIdempotencyReplayStore store;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(
                PaymentIdempotencyRepository.class
        );
        store = new PaymentIdempotencyReplayStore(
                repository
        );
    }

    @Test
    void reservesANewRequest() {
        when(repository.findByOperationAndIdempotencyKey(
                OPERATION,
                KEY
        )).thenReturn(Optional.empty());

        PaymentIdempotencyDecision decision =
                store.begin(
                        OPERATION,
                        KEY,
                        HASH,
                        NOW
                );

        assertThat(decision.kind())
                .isEqualTo(
                        PaymentIdempotencyDecision.Kind.NEW
                );
        verify(repository).saveAndFlush(
                Mockito.any(PaymentIdempotencyEntity.class)
        );
    }

    @Test
    void replaysACompletedResult() {
        UUID paymentId = UUID.randomUUID();
        PaymentIdempotencyEntity entity =
                PaymentIdempotencyEntity.start(
                        OPERATION,
                        KEY,
                        HASH,
                        NOW
                );
        entity.complete(
                paymentId,
                "ACCEPTED",
                "{\"result\":\"accepted\"}",
                NOW.plusSeconds(1)
        );

        when(repository.findByOperationAndIdempotencyKey(
                OPERATION,
                KEY
        )).thenReturn(Optional.of(entity));

        PaymentIdempotencyDecision decision =
                store.begin(
                        OPERATION,
                        KEY,
                        HASH,
                        NOW.plusSeconds(2)
                );

        assertThat(decision.kind())
                .isEqualTo(
                        PaymentIdempotencyDecision.Kind.REPLAY
                );
        assertThat(decision.paymentId())
                .isEqualTo(paymentId);
        assertThat(decision.responseStatus())
                .isEqualTo("ACCEPTED");
        assertThat(decision.responsePayload())
                .isEqualTo("{\"result\":\"accepted\"}");
    }

    @Test
    void rejectsSameKeyWithDifferentRequestHash() {
        PaymentIdempotencyEntity entity =
                PaymentIdempotencyEntity.start(
                        OPERATION,
                        KEY,
                        HASH,
                        NOW
                );

        when(repository.findByOperationAndIdempotencyKey(
                OPERATION,
                KEY
        )).thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                store.begin(
                        OPERATION,
                        KEY,
                        "b".repeat(64),
                        NOW.plusSeconds(1)
                )
        ).isInstanceOf(
                PaymentIdempotencyConflictException.class
        );
    }
}
