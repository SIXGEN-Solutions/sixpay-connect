package com.sixpay.payment.infrastructure.idempotency;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class PaymentConfirmationIdempotencyTransactions {

    private final PaymentIdempotencyConcurrencyCoordinator coordinator;
    private final PaymentIdempotencyReplayStore replayStore;

    public PaymentConfirmationIdempotencyTransactions(
            PaymentIdempotencyConcurrencyCoordinator coordinator,
            PaymentIdempotencyReplayStore replayStore
    ) {
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Idempotency concurrency coordinator"
        );
        this.replayStore = Objects.requireNonNull(
                replayStore,
                "Idempotency replay store"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BeginResult begin(
            String operation,
            String idempotencyKey,
            String preferredHash,
            List<String> acceptedHashes,
            Instant startedAt
    ) {
        return coordinator.executeLocked(
                operation,
                idempotencyKey,
                () -> {
                    PaymentIdempotencyReplayStore.MatchingBeginResult result =
                            replayStore.beginMatchingHashes(
                                    operation,
                                    idempotencyKey,
                                    preferredHash,
                                    acceptedHashes,
                                    startedAt
                            );
                    return new BeginResult(
                            result.decision(),
                            result.requestHash()
                    );
                }
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String responseStatus,
            String responsePayload,
            Instant completedAt
    ) {
        replayStore.complete(
                operation,
                idempotencyKey,
                requestHash,
                paymentId,
                responseStatus,
                responsePayload,
                completedAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOutcomeUnknown(
            String operation,
            String idempotencyKey,
            String requestHash,
            UUID paymentId,
            String recoveryReference,
            String recoveryReason,
            Instant unknownOutcomeAt
    ) {
        replayStore.markOutcomeUnknown(
                operation,
                idempotencyKey,
                requestHash,
                paymentId,
                recoveryReference,
                recoveryReason,
                unknownOutcomeAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(
            String operation,
            String idempotencyKey,
            String requestHash,
            String reason,
            Instant failedAt
    ) {
        replayStore.fail(
                operation,
                idempotencyKey,
                requestHash,
                reason,
                failedAt
        );
    }

    public record BeginResult(
            PaymentIdempotencyDecision decision,
            String requestHash
    ) {
        public BeginResult {
            decision = Objects.requireNonNull(
                    decision,
                    "Idempotency decision"
            );
            if (requestHash == null
                    || !requestHash.matches("^[0-9a-f]{64}$")) {
                throw new IllegalArgumentException(
                        "Effective request hash is invalid"
                );
            }
        }
    }
}
