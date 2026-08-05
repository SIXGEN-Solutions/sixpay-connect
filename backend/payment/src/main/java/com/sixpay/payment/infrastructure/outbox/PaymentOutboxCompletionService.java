package com.sixpay.payment.infrastructure.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Completes one claimed outbox row in a short transaction.
 * Claim ownership is checked to prevent a stale worker from acknowledging an
 * event already reclaimed by another worker.
 */
@Component
public final class PaymentOutboxCompletionService {

    private final PaymentOutboxRepository repository;
    private final TransactionTemplate transactionTemplate;

    public PaymentOutboxCompletionService(
            PaymentOutboxRepository repository,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository is required"
        );
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(
                        transactionManager,
                        "transactionManager is required"
                )
        );
    }

    public void markPublished(
            UUID eventId,
            String claimOwner,
            Instant completedAt
    ) {
        Objects.requireNonNull(completedAt, "completedAt is required");
        complete(eventId, claimOwner, entity -> entity.markPublished(completedAt));
    }

    public void markRetryableFailure(
            UUID eventId,
            String claimOwner,
            String reason,
            Instant failedAt,
            Instant retryAt
    ) {
        Objects.requireNonNull(failedAt, "failedAt is required");
        Objects.requireNonNull(retryAt, "retryAt is required");
        complete(
                eventId,
                claimOwner,
                entity -> entity.markFailed(reason, failedAt, retryAt)
        );
    }

    public void markDead(
            UUID eventId,
            String claimOwner,
            String reason,
            Instant failedAt
    ) {
        Objects.requireNonNull(failedAt, "failedAt is required");
        complete(
                eventId,
                claimOwner,
                entity -> entity.markDead(reason, failedAt)
        );
    }

    private void complete(
            UUID eventId,
            String claimOwner,
            Consumer<PaymentOutboxEntity> transition
    ) {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(transition, "transition is required");
        String owner = requireOwner(claimOwner);

        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEntity entity = repository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Claimed Payment outbox event not found: " + eventId
                    ));

            if (entity.status() != PaymentOutboxEntity.Status.PROCESSING) {
                throw new IllegalStateException(
                        "Outbox completion requires PROCESSING status for event "
                                + eventId
                );
            }

            if (!owner.equals(entity.claimedBy())) {
                throw new IllegalStateException(
                        "Outbox claim ownership changed for event " + eventId
                );
            }

            transition.accept(entity);
            repository.flush();
        });
    }

    private static String requireOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("claimOwner must not be blank");
        }
        return owner.strip();
    }
}
