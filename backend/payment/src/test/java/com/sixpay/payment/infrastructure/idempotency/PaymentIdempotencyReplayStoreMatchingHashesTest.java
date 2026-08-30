package com.sixpay.payment.infrastructure.idempotency;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PaymentIdempotencyReplayStoreMatchingHashesTest {

    @Test
    void recognizesExistingHashProducedByRetainedHmacKey() {
        PaymentIdempotencyRepository repository =
                Mockito.mock(PaymentIdempotencyRepository.class);
        PaymentIdempotencyReplayStore store =
                new PaymentIdempotencyReplayStore(repository);

        String oldHash = "a".repeat(64);
        String currentHash = "b".repeat(64);

        PaymentIdempotencyEntity entity =
                PaymentIdempotencyEntity.start(
                        "PAYMENT_CONFIRMATION_VERIFY",
                        "idem-verify-0001",
                        oldHash,
                        Instant.parse("2026-08-30T12:00:00Z")
                );

        when(repository.findByOperationAndIdempotencyKey(
                "PAYMENT_CONFIRMATION_VERIFY",
                "idem-verify-0001"
        )).thenReturn(Optional.of(entity));

        PaymentIdempotencyReplayStore.MatchingBeginResult result =
                store.beginMatchingHashes(
                        "PAYMENT_CONFIRMATION_VERIFY",
                        "idem-verify-0001",
                        currentHash,
                        List.of(currentHash, oldHash),
                        Instant.parse("2026-08-30T12:01:00Z")
                );

        assertThat(result.decision().kind())
                .isEqualTo(
                        PaymentIdempotencyDecision.Kind.IN_PROGRESS
                );
        assertThat(result.requestHash())
                .isEqualTo(oldHash);
    }

    @Test
    void rejectsSameKeyWhenNoSecureFingerprintMatches() {
        PaymentIdempotencyRepository repository =
                Mockito.mock(PaymentIdempotencyRepository.class);
        PaymentIdempotencyReplayStore store =
                new PaymentIdempotencyReplayStore(repository);

        PaymentIdempotencyEntity entity =
                PaymentIdempotencyEntity.start(
                        "PAYMENT_CONFIRMATION_VERIFY",
                        "idem-verify-0002",
                        "a".repeat(64),
                        Instant.parse("2026-08-30T12:00:00Z")
                );

        when(repository.findByOperationAndIdempotencyKey(
                "PAYMENT_CONFIRMATION_VERIFY",
                "idem-verify-0002"
        )).thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                store.beginMatchingHashes(
                        "PAYMENT_CONFIRMATION_VERIFY",
                        "idem-verify-0002",
                        "b".repeat(64),
                        List.of("b".repeat(64), "c".repeat(64)),
                        Instant.parse("2026-08-30T12:01:00Z")
                )
        ).isInstanceOf(
                PaymentIdempotencyConflictException.class
        );
    }
}
