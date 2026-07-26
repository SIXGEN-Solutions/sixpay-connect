package com.sixpay.sharedkernel.domain.model;

import com.sixpay.common.validation.Preconditions;
import com.sixpay.sharedkernel.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for aggregate roots capable of registering domain events.
 *
 * @param <ID> aggregate identifier type
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Registers an event produced by this aggregate.
     *
     * @param domainEvent event to register
     */
    protected final void registerDomainEvent(
            DomainEvent domainEvent
    ) {
        domainEvents.add(
                Preconditions.requireNonNull(
                        domainEvent,
                        "Domain event must not be null"
                )
        );
    }

    /**
     * Returns the currently registered events without removing them.
     *
     * @return immutable event collection
     */
    public final List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    /**
     * Returns and removes all events currently registered by the aggregate.
     *
     * @return immutable collection of released events
     */
    public final List<DomainEvent> releaseDomainEvents() {
        List<DomainEvent> releasedEvents =
                List.copyOf(domainEvents);

        domainEvents.clear();

        return releasedEvents;
    }

    /**
     * Removes all unpublished events.
     */
    public final void clearDomainEvents() {
        domainEvents.clear();
    }
}