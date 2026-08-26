package com.sixpay.common.time;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Time provider backed by a Java Clock.
 */
public final class SystemTimeProvider implements TimeProvider {

    private final Clock clock;

    /**
     * Creates a time provider using the UTC system clock.
     */
    public SystemTimeProvider() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a time provider using the supplied clock.
     *
     * @param clock clock to use
     */
    public SystemTimeProvider(Clock clock) {
        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    @Override
    public Instant now() {
        return clock.instant();
    }
}