package com.sixpay.bootstrap.integration.customer.outbox;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxCompletionService;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaim;
import com.sixpay.payment.infrastructure.outbox.claim.PaymentOutboxClaimService;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxEventDeserializer;
import com.sixpay.payment.infrastructure.outbox.serialization.PaymentOutboxEventTypeRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Claims and delivers one bounded batch of Payment outbox events.
 * No row is published before Customer Observation succeeds.
 */
public final class PaymentObservedCustomerOutboxDispatcher {

    private final PaymentOutboxClaimService claimService;
    private final PaymentOutboxEventDeserializer deserializer;
    private final PaymentObservedCustomerOutboxHandler handler;
    private final PaymentObservedCustomerOutboxFailureClassifier failureClassifier;
    private final PaymentOutboxCompletionService completionService;

    public PaymentObservedCustomerOutboxDispatcher(
            PaymentOutboxClaimService claimService,
            PaymentOutboxEventDeserializer deserializer,
            PaymentObservedCustomerOutboxHandler handler,
            PaymentObservedCustomerOutboxFailureClassifier failureClassifier,
            PaymentOutboxCompletionService completionService
    ) {
        this.claimService = Objects.requireNonNull(claimService, "claimService is required");
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer is required");
        this.handler = Objects.requireNonNull(handler, "handler is required");
        this.failureClassifier = Objects.requireNonNull(
                failureClassifier,
                "failureClassifier is required"
        );
        this.completionService = Objects.requireNonNull(
                completionService,
                "completionService is required"
        );
    }

    public List<PaymentObservedCustomerOutboxResult> dispatchAvailable(
            Instant now,
            String workerId,
            int batchSize,
            Duration processingTimeout,
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff
    ) {
        validateConfiguration(
                now,
                workerId,
                batchSize,
                processingTimeout,
                maxAttempts,
                initialBackoff,
                maxBackoff
        );

        List<PaymentOutboxClaim> claims = claimService.claimAvailableByEventType(
                PaymentOutboxEventTypeRegistry.OBSERVED_CUSTOMER_PROJECTION_TYPE,
                now,
                now.minus(processingTimeout),
                batchSize,
                workerId
        );

        List<PaymentObservedCustomerOutboxResult> results =
                new ArrayList<>(claims.size());

        for (PaymentOutboxClaim claim : claims) {
            results.add(
                    dispatchOne(
                            claim,
                            now,
                            maxAttempts,
                            initialBackoff,
                            maxBackoff
                    )
            );
        }

        return List.copyOf(results);
    }

    private PaymentObservedCustomerOutboxResult dispatchOne(
            PaymentOutboxClaim claim,
            Instant now,
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff
    ) {
        try {
            ObservedCustomerProjectionEvent event =
                    deserializer.deserialize(claim.payload());

            validateEnvelopeIdentity(claim, event);
            handler.handle(event);

            completionService.markPublished(
                    claim.eventId(),
                    claim.claimedBy(),
                    now
            );

            return new PaymentObservedCustomerOutboxResult(
                    claim.eventId(),
                    PaymentObservedCustomerOutboxResult.Outcome.PUBLISHED,
                    claim.attempt(),
                    null,
                    null
            );
        } catch (RuntimeException failure) {
            var classification = failureClassifier.classify(failure);

            if (classification.retryable()
                    && claim.attempt() < maxAttempts) {
                Instant retryAt = now.plus(
                        backoffForAttempt(
                                claim.attempt(),
                                initialBackoff,
                                maxBackoff
                        )
                );

                completionService.markRetryableFailure(
                        claim.eventId(),
                        claim.claimedBy(),
                        classification.errorType(),
                        now,
                        retryAt
                );

                return new PaymentObservedCustomerOutboxResult(
                        claim.eventId(),
                        PaymentObservedCustomerOutboxResult.Outcome.RETRY_SCHEDULED,
                        claim.attempt(),
                        classification.errorType(),
                        retryAt
                );
            }

            completionService.markDead(
                    claim.eventId(),
                    claim.claimedBy(),
                    classification.errorType(),
                    now
            );

            return new PaymentObservedCustomerOutboxResult(
                    claim.eventId(),
                    PaymentObservedCustomerOutboxResult.Outcome.DEAD_LETTERED,
                    claim.attempt(),
                    classification.errorType(),
                    null
            );
        }
    }

    private static void validateEnvelopeIdentity(
            PaymentOutboxClaim claim,
            ObservedCustomerProjectionEvent event
    ) {
        if (!claim.eventId().equals(event.eventId())) {
            throw new IllegalArgumentException(
                    "Outbox row and payload eventId differ"
            );
        }

        if (!claim.aggregateId().equals(event.paymentId())) {
            throw new IllegalArgumentException(
                    "Outbox row and payload aggregateId differ"
            );
        }

        if (!claim.correlationId().equals(event.correlationId())) {
            throw new IllegalArgumentException(
                    "Outbox row and payload correlationId differ"
            );
        }

        if (claim.schemaVersion() != event.eventVersion()) {
            throw new IllegalArgumentException(
                    "Outbox row and payload version differ"
            );
        }
    }

    private static Duration backoffForAttempt(
            int attempt,
            Duration initialBackoff,
            Duration maxBackoff
    ) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);

        Duration calculated;
        try {
            calculated = initialBackoff.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            return maxBackoff;
        }

        return calculated.compareTo(maxBackoff) > 0
                ? maxBackoff
                : calculated;
    }

    private static void validateConfiguration(
            Instant now,
            String workerId,
            int batchSize,
            Duration processingTimeout,
            int maxAttempts,
            Duration initialBackoff,
            Duration maxBackoff
    ) {
        Objects.requireNonNull(now, "now is required");

        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }

        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least one");
        }

        requirePositive(processingTimeout, "processingTimeout");

        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least one");
        }

        requirePositive(initialBackoff, "initialBackoff");
        requirePositive(maxBackoff, "maxBackoff");

        if (initialBackoff.compareTo(maxBackoff) > 0) {
            throw new IllegalArgumentException(
                    "initialBackoff must not exceed maxBackoff"
            );
        }
    }

    private static void requirePositive(
            Duration duration,
            String field
    ) {
        Objects.requireNonNull(duration, field + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
