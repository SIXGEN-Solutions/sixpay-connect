package com.sixpay.sharedkernel.domain.model;

import com.sixpay.common.validation.Preconditions;

import java.util.Objects;

/**
 * Base class for domain entities identified by a stable identifier.
 *
 * @param <ID> entity identifier type
 */
public abstract class Entity<ID> {

    private final ID id;

    protected Entity(ID id) {
        this.id = Preconditions.requireNonNull(
                id,
                "Entity identifier must not be null"
        );
    }

    public final ID id() {
        return id;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        Entity<?> entity = (Entity<?>) other;

        return Objects.equals(id, entity.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id);
    }
}