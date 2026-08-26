package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable replay store for Payment idempotency.
 *
 * <p>Every mutating call requires an existing transaction. Callers must first
 * enter {@link PaymentIdempotencyConcurrencyCoordinator#executeLocked} for
 * the same operation and key.</p>
 */
@Repository
public class PaymentIdempotencyReplayStore {

    private final PaymentIdempotencyRepository repository;

    public PaymentIdempotencyReplayStore(
            PaymentIdempotencyRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Payment idempotency repository"
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentIdempotencyDecision begin(
            String operation,
            String idempotencyKey,
            String requestHash,
            Instant startedAt
    ) {
        validate(operation, idempotencyKey, requestHash);
        Objects.requireNonNull(
                startedAt,
                "Idempotency start instant"
        );

        Optional<PaymentIdempotencyEntity> existing =
                repository.findByOperationAndIdempotencyKey(
                        operation,
                        idempotencyKey
                );

        if (existing.isEmpty()) {
            repository.saveAndFlush(
                    PaymentIdempotencyEntity.start(
                            operation,
                            idempotencyKey,
                            requestHash,
                            startedAt
                    )
            );
            return PaymentIdempotencyDecision.newRequest();
        }

        PaymentIdempotencyEntity entity =
                existing.orElseThrow();

        requireSameHash(
                entity,
                operation,
                idempotencyKey,
                requestHash
        );

        return switch (entity.status()) {
            case COMPLETED ->
                    PaymentIdempotencyDecision.replay(entity);
            case IN_PROGRESS ->
                    PaymentIdempotencyDecision.inProgress();
            case FAILED -> {
                entity.restart(startedAt);
                repository.saveAndFlush(entity);
                yield PaymentIdempotencyDecision.newRequest();
            }
        };
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void complete(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String responseStatus,
            String responsePayload,
            Instant completedAt
    ) {
        PaymentIdempotencyEntity entity =
                requireExisting(operation, idempotencyKey);

        requireSameHash(
                entity,
                operation,
                idempotencyKey,
                requestHash
        );

        entity.complete(
                paymentId,
                responseStatus,
                responsePayload,
                completedAt
        );
        repository.saveAndFlush(entity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void fail(
            String operation,
            String idempotencyKey,
            String requestHash,
            String failureReason,
            Instant failedAt
    ) {
        PaymentIdempotencyEntity entity =
                requireExisting(operation, idempotencyKey);

        requireSameHash(
                entity,
                operation,
                idempotencyKey,
                requestHash
        );

        entity.fail(failureReason, failedAt);
        repository.saveAndFlush(entity);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentIdempotencyDecision> findReplay(
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        validate(operation, idempotencyKey, requestHash);

        return repository
                .findByOperationAndIdempotencyKey(
                        operation,
                        idempotencyKey
                )
                .map(entity -> {
                    requireSameHash(
                            entity,
                            operation,
                            idempotencyKey,
                            requestHash
                    );

                    return entity.status()
                            == PaymentIdempotencyEntity.Status.COMPLETED
                            ? PaymentIdempotencyDecision.replay(entity)
                            : PaymentIdempotencyDecision.inProgress();
                });
    }

    private PaymentIdempotencyEntity requireExisting(
            String operation,
            String idempotencyKey
    ) {
        return repository
                .findByOperationAndIdempotencyKey(
                        operation,
                        idempotencyKey
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Idempotency record does not exist"
                        )
                );
    }

    private static void requireSameHash(
            PaymentIdempotencyEntity entity,
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        if (!entity.requestHash().equals(requestHash)) {
            throw new PaymentIdempotencyConflictException(
                    operation,
                    idempotencyKey
            );
        }
    }

    private static void validate(
            String operation,
            String idempotencyKey,
            String requestHash
    ) {
        requireText(
                operation,
                160,
                "Idempotency operation"
        );
        requireText(
                idempotencyKey,
                150,
                "Idempotency key"
        );

        if (requestHash == null
                || !requestHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Request hash must be a lowercase SHA-256 value"
            );
        }
    }

    private static void requireText(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " must be non-blank and at most "
                            + maximumLength + " characters"
            );
        }
    }
}
