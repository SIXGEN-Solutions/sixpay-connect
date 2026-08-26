package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.input.OperationalNotificationOperationsUseCase;
import com.sixpay.notification.application.port.input.OperationalNotificationReplayCommand;
import com.sixpay.notification.application.port.input.OperationalNotificationReplayResult;
import com.sixpay.notification.application.port.input.OperationalNotificationStatusView;
import com.sixpay.notification.application.port.output.NotificationReplayIdGenerator;
import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import com.sixpay.notification.domain.model.NotificationReplayAudit;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.NotificationReplayRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class OperationalNotificationOperationsService
        implements OperationalNotificationOperationsUseCase {

    private final OperationalNotificationRepository repository;
    private final OperationalNotificationOperationsRepository
            operationsRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final NotificationReplayRepository replayRepository;
    private final NotificationReplayIdGenerator replayIdGenerator;
    private final OperationalNotificationOperationsTelemetry telemetry;
    private final Clock clock;

    public OperationalNotificationOperationsService(
            OperationalNotificationRepository repository,
            OperationalNotificationOperationsRepository operationsRepository,
            NotificationAttemptRepository attemptRepository,
            NotificationReplayRepository replayRepository,
            NotificationReplayIdGenerator replayIdGenerator,
            OperationalNotificationOperationsTelemetry telemetry,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository"
        );
        this.operationsRepository = Objects.requireNonNull(
                operationsRepository,
                "operationsRepository"
        );
        this.attemptRepository = Objects.requireNonNull(
                attemptRepository,
                "attemptRepository"
        );
        this.replayRepository = Objects.requireNonNull(
                replayRepository,
                "replayRepository"
        );
        this.replayIdGenerator = Objects.requireNonNull(
                replayIdGenerator,
                "replayIdGenerator"
        );
        this.telemetry = Objects.requireNonNull(
                telemetry,
                "telemetry"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock"
        );
    }

    @Override
    public Optional<OperationalNotificationStatusView> status(
            UUID notificationId
    ) {
        Objects.requireNonNull(
                notificationId,
                "notificationId"
        );

        return repository.findById(
                        notificationId
                )
                .map(this::toView);
    }

    @Override
    public List<OperationalNotificationStatusView> findByStatus(
            NotificationDeliveryStatus status,
            int limit
    ) {
        Objects.requireNonNull(
                status,
                "status"
        );

        if (limit <= 0 || limit > 500) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 500"
            );
        }

        return operationsRepository
                .findIdsByStatus(
                        status,
                        limit
                )
                .stream()
                .map(repository::findById)
                .flatMap(Optional::stream)
                .map(this::toView)
                .toList();
    }

    @Override
    public OperationalNotificationReplayResult replay(
            UUID notificationId,
            OperationalNotificationReplayCommand command
    ) {
        Objects.requireNonNull(
                notificationId,
                "notificationId"
        );
        Objects.requireNonNull(
                command,
                "command"
        );

        OperationalNotificationDelivery existing =
                repository.findById(
                                notificationId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Operational notification not found"
                                )
                        );

        if (existing.intent().status()
                != NotificationDeliveryStatus.DEAD_LETTERED) {
            throw new IllegalStateException(
                    "Only DEAD_LETTERED notifications "
                            + "can be replayed"
            );
        }

        Instant replayedAt = clock.instant();

        NotificationReplayAudit audit =
                new NotificationReplayAudit(
                        replayIdGenerator.nextId(),
                        notificationId,
                        command.operatorReference(),
                        command.reason(),
                        existing.intent().status(),
                        replayedAt
                );

        OperationalNotificationDelivery replayed =
                replayRepository
                        .replayDeadLetter(
                                audit
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Notification replay was not claimed"
                                )
                        );

        telemetry.recordReplay();

        return new OperationalNotificationReplayResult(
                notificationId,
                replayed.intent().status(),
                replayed.replayCount(),
                replayedAt
        );
    }

    private OperationalNotificationStatusView toView(
            OperationalNotificationDelivery delivery
    ) {
        var intent = delivery.intent();

        return new OperationalNotificationStatusView(
                intent.notificationId(),
                intent.source().triggerType(),
                intent.source().sourceId(),
                intent.recipient().reference(),
                intent.channel(),
                intent.templateKey(),
                intent.status(),
                delivery.attemptCount(),
                delivery.cycleAttemptCount(),
                delivery.replayCount(),
                delivery.nextAttemptAt(),
                delivery.lastAttemptAt(),
                delivery.deliveredAt(),
                delivery.lastReplayAt(),
                delivery.lastErrorCode(),
                delivery.providerReference(),
                intent.createdAt(),
                intent.correlationId(),
                attemptRepository.findByNotificationId(
                        intent.notificationId()
                )
        );
    }
}
