package com.sixpay.notification.domain.policy;

import com.sixpay.notification.domain.model.NotificationDeliveryStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDeliveryLifecycleTest {

    private final NotificationDeliveryLifecycle lifecycle =
            new NotificationDeliveryLifecycle();

    @Test
    void supportsNominalEmailLifecycle() {
        assertTrue(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.PENDING,
                        NotificationDeliveryStatus.DISPATCHING
                )
        );

        assertTrue(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.DISPATCHING,
                        NotificationDeliveryStatus.ACCEPTED
                )
        );

        assertTrue(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.ACCEPTED,
                        NotificationDeliveryStatus.DELIVERED
                )
        );
    }

    @Test
    void deliveredIsTerminal() {
        assertFalse(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.DELIVERED,
                        NotificationDeliveryStatus.DISPATCHING
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> lifecycle.requireTransition(
                        NotificationDeliveryStatus.DELIVERED,
                        NotificationDeliveryStatus.DISPATCHING
                )
        );
    }

    @Test
    void retryableFailureCanBeRedispatchedOrDeadLettered() {
        assertTrue(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.FAILED_RETRYABLE,
                        NotificationDeliveryStatus.DISPATCHING
                )
        );

        assertTrue(
                lifecycle.canTransition(
                        NotificationDeliveryStatus.FAILED_RETRYABLE,
                        NotificationDeliveryStatus.DEAD_LETTERED
                )
        );
    }
}
