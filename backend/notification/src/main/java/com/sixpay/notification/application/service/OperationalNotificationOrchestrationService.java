package com.sixpay.notification.application.service;

import com.sixpay.notification.application.port.input.NotificationRegistrationResult;
import com.sixpay.notification.application.port.input.OperationalNotificationOrchestrationUseCase;
import com.sixpay.notification.application.port.input.OperationalNotificationTriggerUseCase;
import com.sixpay.notification.domain.model.OperationalNotificationDelivery;
import com.sixpay.notification.domain.model.OperationalNotificationTrigger;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;

import java.util.Objects;

public final class OperationalNotificationOrchestrationService
        implements OperationalNotificationOrchestrationUseCase {

    private final OperationalNotificationTriggerUseCase planner;
    private final OperationalNotificationRepository repository;

    public OperationalNotificationOrchestrationService(
            OperationalNotificationTriggerUseCase planner,
            OperationalNotificationRepository repository
    ) {
        this.planner = Objects.requireNonNull(
                planner,
                "planner"
        );
        this.repository = Objects.requireNonNull(
                repository,
                "repository"
        );
    }

    @Override
    public NotificationRegistrationResult accept(
            OperationalNotificationTrigger trigger
    ) {
        try {
            var intents = planner.plan(
                    Objects.requireNonNull(
                            trigger,
                            "trigger"
                    )
            );

            int persisted = 0;

            for (var intent : intents) {
                var result = repository.saveIfAbsent(
                        OperationalNotificationDelivery
                                .pending(intent)
                );

                if (result.created()) {
                    persisted++;
                }
            }

            return NotificationRegistrationResult.success(
                    intents.size(),
                    persisted
            );
        } catch (RuntimeException exception) {
            /*
             * This boundary is intentionally non-throwing.
             *
             * A Notification subsystem failure must not roll back or fail the
             * Payment/Accounting business transaction that emitted the source
             * fact. Source-side outbox/post-commit composition may observe the
             * failure result and retry independently.
             */
            return NotificationRegistrationResult.failure(
                    "NOTIFICATION_REGISTRATION_FAILED"
            );
        }
    }
}
