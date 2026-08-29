package com.sixpay.notification.application.port.output;

import com.sixpay.notification.application.model.NotificationDeliveryRegistration;
import com.sixpay.notification.application.model.NotificationDeliveryAttempt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence boundary for notification idempotence and delivery tracking.
 */
public interface NotificationDeliveryStore {

    /**
     * Atomically registers the event as PROCESSING.
     *
     * @return true when this event was registered by this call; false when
     *         the eventId was already known
     */
    boolean tryStart(NotificationDeliveryRegistration registration);

    /**
     * Atomically claims due deliveries and increments their attempt counter.
     * SENT and DEAD deliveries are never eligible.
     */
    List<NotificationDeliveryAttempt> claimDue(
            Instant now,
            int batchSize
    );

    void markSent(UUID eventId, Instant sentAt);

    void markFailed(
            UUID eventId,
            String error,
            Instant failedAt,
            Instant nextAttemptAt
    );

    void markDead(
            UUID eventId,
            String error,
            Instant failedAt
    );
}
