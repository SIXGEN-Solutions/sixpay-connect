package com.sixpay.sharedkernel.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityTest {

    @Test
    void shouldConsiderEntitiesWithSameIdentifierEqual() {
        UUID identifier = UUID.randomUUID();

        TestEntity first = new TestEntity(identifier);
        TestEntity second = new TestEntity(identifier);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldConsiderEntitiesWithDifferentIdentifiersDifferent() {
        TestEntity first = new TestEntity(UUID.randomUUID());
        TestEntity second = new TestEntity(UUID.randomUUID());

        assertNotEquals(first, second);
    }

    @Test
    void shouldRejectNullIdentifier() {
        assertThrows(
                NullPointerException.class,
                () -> new TestEntity(null)
        );
    }

    private static final class TestEntity extends Entity<UUID> {

        private TestEntity(UUID id) {
            super(id);
        }
    }
}