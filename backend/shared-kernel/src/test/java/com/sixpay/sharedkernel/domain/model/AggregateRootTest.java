package com.sixpay.sharedkernel.domain.model;

import com.sixpay.sharedkernel.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregateRootTest {

    @Test
    void shouldRegisterDomainEvent() {
        TestAggregate aggregate =
                new TestAggregate(UUID.randomUUID());

        aggregate.performOperation();

        assertEquals(1, aggregate.domainEvents().size());
        assertEquals(
                "TestOperationCompleted",
                aggregate.domainEvents().getFirst().eventType()
        );
    }

    @Test
    void shouldReleaseAndClearDomainEvents() {
        TestAggregate aggregate =
                new TestAggregate(UUID.randomUUID());

        aggregate.performOperation();

        List<DomainEvent> releasedEvents =
                aggregate.releaseDomainEvents();

        assertEquals(1, releasedEvents.size());
        assertTrue(aggregate.domainEvents().isEmpty());
    }

    @Test
    void shouldRejectNullDomainEvent() {
        TestAggregate aggregate =
                new TestAggregate(UUID.randomUUID());

        assertThrows(
                NullPointerException.class,
                aggregate::registerNullEvent
        );
    }

    private static final class TestAggregate
            extends AggregateRoot<UUID> {

        private TestAggregate(UUID id) {
            super(id);
        }

        private void performOperation() {
            registerDomainEvent(
                    new TestOperationCompleted(
                            UUID.randomUUID(),
                            Instant.parse("2026-07-26T12:00:00Z"),
                            id()
                    )
            );
        }

        private void registerNullEvent() {
            registerDomainEvent(null);
        }
    }

    private record TestOperationCompleted(
            UUID eventId,
            Instant occurredAt,
            UUID aggregateId
    ) implements DomainEvent {
    }
}