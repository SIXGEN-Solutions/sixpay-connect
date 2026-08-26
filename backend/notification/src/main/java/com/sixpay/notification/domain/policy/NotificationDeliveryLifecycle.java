package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class NotificationDeliveryLifecycle {

    private static final Map<
            NotificationDeliveryStatus,
            Set<NotificationDeliveryStatus>
            > ALLOWED = Map.of(
            NotificationDeliveryStatus.PENDING,
            EnumSet.of(
                    NotificationDeliveryStatus.DISPATCHING
            ),
            NotificationDeliveryStatus.DISPATCHING,
            EnumSet.of(
                    NotificationDeliveryStatus.ACCEPTED,
                    NotificationDeliveryStatus.FAILED_RETRYABLE,
                    NotificationDeliveryStatus.FAILED_PERMANENT
            ),
            NotificationDeliveryStatus.ACCEPTED,
            EnumSet.of(
                    NotificationDeliveryStatus.DELIVERED,
                    NotificationDeliveryStatus.FAILED_RETRYABLE,
                    NotificationDeliveryStatus.FAILED_PERMANENT
            ),
            NotificationDeliveryStatus.FAILED_RETRYABLE,
            EnumSet.of(
                    NotificationDeliveryStatus.DISPATCHING,
                    NotificationDeliveryStatus.DEAD_LETTERED
            ),
            NotificationDeliveryStatus.DELIVERED,
            Set.of(),
            NotificationDeliveryStatus.FAILED_PERMANENT,
            Set.of(),
            NotificationDeliveryStatus.DEAD_LETTERED,
            Set.of()
    );

    public boolean canTransition(
            NotificationDeliveryStatus from,
            NotificationDeliveryStatus to
    ) {
        if (from == null || to == null) {
            return false;
        }

        return ALLOWED
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    public void requireTransition(
            NotificationDeliveryStatus from,
            NotificationDeliveryStatus to
    ) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                    "Unsupported notification lifecycle transition: "
                            + from
                            + " -> "
                            + to
            );
        }
    }
}
