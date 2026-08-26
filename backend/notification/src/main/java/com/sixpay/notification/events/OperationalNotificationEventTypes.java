package com.sixpay.notification.events;

/**
 * Logical source event names consumed by the Notification capability.
 *
 * These constants do not imply Kafka transport. While modules are co-deployed,
 * composition adapters may invoke the Notification receiving use case
 * in-process.
 */
public final class OperationalNotificationEventTypes {

    public static final String PAYMENT_POSTED =
            "payment.posted.v1";

    public static final String ACCOUNTING_BATCH_COMPLETED =
            "accounting.batch.completed.v1";

    private OperationalNotificationEventTypes() {
    }
}
