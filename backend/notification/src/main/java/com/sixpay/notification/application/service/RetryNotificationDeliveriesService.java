package com.sixpay.notification.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.notification.application.model.NotificationDeliveryAttempt;
import com.sixpay.notification.application.model.PartnerDecisionNotification;
import com.sixpay.notification.application.model.PartnerDecisionNotification.Decision;
import com.sixpay.notification.application.port.input.RetryNotificationDeliveriesUseCase;
import com.sixpay.notification.application.port.output.NotificationDeliveryStore;
import com.sixpay.notification.application.port.output.PartnerNotificationSender;

import java.time.Instant;
import java.util.Objects;

public final class RetryNotificationDeliveriesService
        implements RetryNotificationDeliveriesUseCase {

    private final NotificationDeliveryStore deliveryStore;
    private final PartnerNotificationSender sender;
    private final TimeProvider timeProvider;
    private final NotificationRetryPolicy retryPolicy;
    private final int batchSize;

    public RetryNotificationDeliveriesService(
            NotificationDeliveryStore deliveryStore,
            PartnerNotificationSender sender,
            TimeProvider timeProvider,
            NotificationRetryPolicy retryPolicy,
            int batchSize
    ) {
        this.deliveryStore = Objects.requireNonNull(deliveryStore);
        this.sender = Objects.requireNonNull(sender);
        this.timeProvider = Objects.requireNonNull(timeProvider);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }
        this.batchSize = batchSize;
    }

    @Override
    public void retryDueDeliveries() {
        Instant now = timeProvider.now();
        for (var attempt : deliveryStore.claimDue(now, batchSize)) {
            retry(attempt);
        }
    }

    private void retry(NotificationDeliveryAttempt attempt) {
        try {
            sender.send(toNotification(attempt));
            deliveryStore.markSent(
                    attempt.eventId(),
                    timeProvider.now()
            );
        } catch (RuntimeException exception) {
            Instant failedAt = timeProvider.now();
            String error = errorMessage(exception);
            if (retryPolicy.exhausted(attempt.attemptCount())) {
                deliveryStore.markDead(
                        attempt.eventId(),
                        error,
                        failedAt
                );
                return;
            }
            deliveryStore.markFailed(
                    attempt.eventId(),
                    error,
                    failedAt,
                    retryPolicy.nextAttemptAt(
                            failedAt,
                            attempt.attemptCount()
                    )
            );
        }
    }

    private static PartnerDecisionNotification toNotification(
            NotificationDeliveryAttempt attempt
    ) {
        return new PartnerDecisionNotification(
                attempt.eventId(),
                attempt.aggregateId(),
                attempt.recipient(),
                decision(attempt.template()),
                attempt.reason(),
                attempt.correlationId()
        );
    }

    private static Decision decision(String template) {
        return switch (template) {
            case "partner-activated" -> Decision.APPROVED;
            case "partner-rejected" -> Decision.REJECTED;
            case "partner-suspended" -> Decision.SUSPENDED;
            default -> throw new IllegalArgumentException(
                    "unsupported persisted notification template: "
                            + template
            );
        };
    }

    static String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getName();
        }
        return message.length() <= 2000
                ? message
                : message.substring(0, 2000);
    }
}
