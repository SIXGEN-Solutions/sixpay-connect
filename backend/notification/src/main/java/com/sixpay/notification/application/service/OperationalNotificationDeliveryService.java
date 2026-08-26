package com.sixpay.notification.application.service;

import com.sixpay.notification.application.exception.PermanentNotificationDeliveryException;
import com.sixpay.notification.application.exception.RetryableNotificationDeliveryException;
import com.sixpay.notification.application.port.input.NotificationProcessingReport;
import com.sixpay.notification.application.port.input.ProcessOperationalNotificationsUseCase;
import com.sixpay.notification.application.port.output.NotificationAttemptIdGenerator;
import com.sixpay.notification.application.port.output.NotificationDispatchResult;
import com.sixpay.notification.application.port.output.OperationalNotificationDeliveryGateway;
import com.sixpay.notification.domain.model.NotificationAttempt;
import com.sixpay.notification.domain.model.NotificationAttemptOutcome;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.policy.NotificationDeliveryLifecycle;
import com.sixpay.notification.domain.policy.OperationalNotificationRetryPolicy;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OperationalNotificationDeliveryService
        implements ProcessOperationalNotificationsUseCase {

    private final OperationalNotificationRepository repository;
    private final NotificationAttemptRepository attemptRepository;
    private final OperationalNotificationDeliveryGateway gateway;
    private final NotificationAttemptIdGenerator attemptIdGenerator;
    private final NotificationDeliveryLifecycle lifecycle;
    private final OperationalNotificationRetryPolicy retryPolicy;
    private final Clock clock;

    public OperationalNotificationDeliveryService(
            OperationalNotificationRepository repository,
            NotificationAttemptRepository attemptRepository,
            OperationalNotificationDeliveryGateway gateway,
            NotificationAttemptIdGenerator attemptIdGenerator,
            NotificationDeliveryLifecycle lifecycle,
            OperationalNotificationRetryPolicy retryPolicy,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository"
        );
        this.attemptRepository = Objects.requireNonNull(
                attemptRepository,
                "attemptRepository"
        );
        this.gateway = Objects.requireNonNull(
                gateway,
                "gateway"
        );
        this.attemptIdGenerator = Objects.requireNonNull(
                attemptIdGenerator,
                "attemptIdGenerator"
        );
        this.lifecycle = Objects.requireNonNull(
                lifecycle,
                "lifecycle"
        );
        this.retryPolicy = Objects.requireNonNull(
                retryPolicy,
                "retryPolicy"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    @Override
    public NotificationProcessingReport processDue(
            Instant dueAt,
            int batchSize
    ) {
        Objects.requireNonNull(
                dueAt,
                "dueAt"
        );

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be positive"
            );
        }

        var ids = repository.findDueNotificationIds(
                dueAt,
                batchSize
        );

        Counters counters = new Counters(
                ids.size()
        );

        for (UUID notificationId : ids) {
            try {
                processOne(
                        notificationId,
                        dueAt,
                        counters
                );
            } catch (RuntimeException ignored) {
                counters.skipped++;
            }
        }

        return counters.report();
    }

    private void processOne(
            UUID notificationId,
            Instant claimedAt,
            Counters counters
    ) {
        var claimed = repository.claimForDispatch(
                notificationId,
                claimedAt
        );

        if (claimed.isEmpty()) {
            counters.skipped++;
            return;
        }

        OperationalNotificationDelivery delivery =
                claimed.orElseThrow();

        /*
         * claimForDispatch already moved the persisted state to DISPATCHING.
         * A first attempt in a cycle originates from PENDING (initial cycle)
         * or FAILED_RETRYABLE (replayed cycle). Later attempts originate from
         * FAILED_RETRYABLE.
         */
        lifecycle.requireTransition(
                sourceStatusForClaim(delivery),
                NotificationDeliveryStatus.DISPATCHING
        );

        counters.processed++;

        NotificationAttempt started =
                new NotificationAttempt(
                        attemptIdGenerator.nextId(),
                        delivery.intent().notificationId(),
                        delivery.attemptCount(),
                        claimedAt,
                        null,
                        NotificationAttemptOutcome.STARTED,
                        null
                );

        attemptRepository.append(started);

        try {
            NotificationDispatchResult result =
                    gateway.deliver(
                            delivery.intent()
                    );

            Instant completedAt = clock.instant();

            if (result.status()
                    == NotificationDeliveryStatus.DELIVERED) {
                lifecycle.requireTransition(
                        NotificationDeliveryStatus.DISPATCHING,
                        NotificationDeliveryStatus.ACCEPTED
                );
                lifecycle.requireTransition(
                        NotificationDeliveryStatus.ACCEPTED,
                        NotificationDeliveryStatus.DELIVERED
                );

                repository.save(
                        delivery.delivered(
                                completedAt,
                                result.providerReference()
                        )
                );

                appendCompletedAttempt(
                        started,
                        completedAt,
                        NotificationAttemptOutcome.DELIVERED,
                        null
                );

                counters.delivered++;
                return;
            }

            lifecycle.requireTransition(
                    NotificationDeliveryStatus.DISPATCHING,
                    NotificationDeliveryStatus.ACCEPTED
            );

            repository.save(
                    delivery.accepted(
                            result.providerReference()
                    )
            );

            appendCompletedAttempt(
                    started,
                    completedAt,
                    NotificationAttemptOutcome.ACCEPTED,
                    null
            );

            counters.accepted++;
        } catch (PermanentNotificationDeliveryException exception) {
            handlePermanentFailure(
                    delivery,
                    started,
                    exception.errorCode(),
                    counters
            );
        } catch (RetryableNotificationDeliveryException exception) {
            handleRetryableFailure(
                    delivery,
                    started,
                    exception.errorCode(),
                    counters
            );
        } catch (RuntimeException exception) {
            handleRetryableFailure(
                    delivery,
                    started,
                    "UNCLASSIFIED_DELIVERY_FAILURE",
                    counters
            );
        }
    }

    private void handleRetryableFailure(
            OperationalNotificationDelivery delivery,
            NotificationAttempt started,
            String errorCode,
            Counters counters
    ) {
        Instant failedAt = clock.instant();

        if (retryPolicy.exhausted(
                delivery.cycleAttemptCount()
        )) {
            lifecycle.requireTransition(
                    NotificationDeliveryStatus.DISPATCHING,
                    NotificationDeliveryStatus.FAILED_RETRYABLE
            );
            lifecycle.requireTransition(
                    NotificationDeliveryStatus.FAILED_RETRYABLE,
                    NotificationDeliveryStatus.DEAD_LETTERED
            );

            repository.save(
                    delivery.deadLetter(
                            errorCode
                    )
            );

            appendCompletedAttempt(
                    started,
                    failedAt,
                    NotificationAttemptOutcome.FAILED_RETRYABLE,
                    errorCode
            );

            counters.deadLettered++;
            return;
        }

        lifecycle.requireTransition(
                NotificationDeliveryStatus.DISPATCHING,
                NotificationDeliveryStatus.FAILED_RETRYABLE
        );

        repository.save(
                delivery.retryableFailure(
                        retryPolicy.nextAttemptAt(
                                failedAt,
                                delivery.cycleAttemptCount()
                        ),
                        errorCode
                )
        );

        appendCompletedAttempt(
                started,
                failedAt,
                NotificationAttemptOutcome.FAILED_RETRYABLE,
                errorCode
        );

        counters.retryScheduled++;
    }

    private void handlePermanentFailure(
            OperationalNotificationDelivery delivery,
            NotificationAttempt started,
            String errorCode,
            Counters counters
    ) {
        Instant failedAt = clock.instant();

        lifecycle.requireTransition(
                NotificationDeliveryStatus.DISPATCHING,
                NotificationDeliveryStatus.FAILED_PERMANENT
        );

        repository.save(
                delivery.permanentFailure(
                        errorCode
                )
        );

        appendCompletedAttempt(
                started,
                failedAt,
                NotificationAttemptOutcome.FAILED_PERMANENT,
                errorCode
        );

        counters.permanentlyFailed++;
    }

    private void appendCompletedAttempt(
            NotificationAttempt started,
            Instant completedAt,
            NotificationAttemptOutcome outcome,
            String errorCode
    ) {
        attemptRepository.append(
                new NotificationAttempt(
                        started.attemptId(),
                        started.notificationId(),
                        started.attemptNumber(),
                        started.startedAt(),
                        completedAt,
                        outcome,
                        errorCode
                )
        );
    }

    private static NotificationDeliveryStatus sourceStatusForClaim(
            OperationalNotificationDelivery claimed
    ) {
        if (claimed.replayCount() == 0
                && claimed.cycleAttemptCount() == 1
                && claimed.attemptCount() == 1) {
            return NotificationDeliveryStatus.PENDING;
        }

        return NotificationDeliveryStatus.FAILED_RETRYABLE;
    }

    private static final class Counters {

        private final int candidates;
        private int processed;
        private int delivered;
        private int accepted;
        private int retryScheduled;
        private int permanentlyFailed;
        private int deadLettered;
        private int skipped;

        private Counters(
                int candidates
        ) {
            this.candidates = candidates;
        }

        private NotificationProcessingReport report() {
            return new NotificationProcessingReport(
                    candidates,
                    processed,
                    delivered,
                    accepted,
                    retryScheduled,
                    permanentlyFailed,
                    deadLettered,
                    skipped
            );
        }
    }
}
