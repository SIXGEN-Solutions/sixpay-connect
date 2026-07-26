package com.sixpay.sharedkernel.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract implemented by every domain event published
 * by SIXPAY CONNECT aggregates.
 */
public interface DomainEvent {

    /**
     * Unique identifier of this event occurrence.
     *
     * @return event identifier
     */
    UUID eventId();

    /**
     * Time at which the event occurred.
     *
     * @return event occurrence time in UTC
     */
    Instant occurredAt();

    /**
     * Returns the logical event type.
     *
     * @return event type
     */
    default String eventType() {
        return getClass().getSimpleName();
    }
}