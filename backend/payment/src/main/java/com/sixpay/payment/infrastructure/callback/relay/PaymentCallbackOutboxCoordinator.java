package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.infrastructure.callback
        .PaymentCallbackProperties;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public class PaymentCallbackOutboxCoordinator {

    private final PaymentOutboxRepository repository;
    private final PaymentCallbackProperties properties;
    private final TimeProvider timeProvider;

    public PaymentCallbackOutboxCoordinator(
            PaymentOutboxRepository repository,
            PaymentCallbackProperties properties,
            TimeProvider timeProvider
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.properties = Objects.requireNonNull(properties);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Transactional
    public List<ClaimedPaymentOutboxEvent> claim() {
        Instant now = timeProvider.now();
        Instant staleBefore = now.minus(
                properties.getClaimTimeout()
        );

        List<PaymentOutboxEntity> entities =
                repository.lockClaimable(
                        now,
                        staleBefore,
                        properties.getBatchSize()
                );

        entities.forEach(entity ->
                entity.claim(
                        now,
                        properties.getWorkerId()
                )
        );

        repository.flush();

        return entities.stream()
                .map(entity ->
                        new ClaimedPaymentOutboxEvent(
                                entity.eventId(),
                                entity.aggregateId(),
                                entity.eventType(),
                                entity.correlationId(),
                                entity.occurredAt(),
                                entity.attemptCount()
                        )
                )
                .toList();
    }

    @Transactional
    public void markPublished(UUID eventId) {
        PaymentOutboxEntity entity = requireOwned(eventId);
        entity.markPublished(timeProvider.now());
    }

    @Transactional
    public void markFailed(
            UUID eventId,
            int attemptCount,
            Throwable failure
    ) {
        PaymentOutboxEntity entity = requireOwned(eventId);
        Instant now = timeProvider.now();

        if (attemptCount >= properties.getMaxAttempts()) {
            entity.markDead(message(failure), now);
            return;
        }

        entity.markFailed(
                message(failure),
                now,
                now.plus(retryDelay(attemptCount))
        );
    }

    private PaymentOutboxEntity requireOwned(UUID eventId) {
        PaymentOutboxEntity entity = repository
                .findById(eventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Missing claimed outbox event "
                                        + eventId
                        )
                );

        if (entity.status()
                != PaymentOutboxEntity.Status.PROCESSING
                || !properties.getWorkerId()
                .equals(entity.claimedBy())) {
            throw new IllegalStateException(
                    "Outbox event is not owned by this callback worker"
            );
        }

        return entity;
    }

    private Duration retryDelay(int attemptCount) {
        long multiplier = 1L << Math.min(
                Math.max(attemptCount - 1, 0),
                20
        );

        Duration calculated =
                properties.getInitialRetryDelay()
                        .multipliedBy(multiplier);

        return calculated.compareTo(
                properties.getMaximumRetryDelay()
        ) > 0
                ? properties.getMaximumRetryDelay()
                : calculated;
    }

    private static String message(Throwable failure) {
        if (failure == null
                || failure.getMessage() == null
                || failure.getMessage().isBlank()) {
            return "Payment callback delivery failed";
        }
        return failure.getMessage();
    }
}
