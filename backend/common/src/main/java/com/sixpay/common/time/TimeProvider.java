package com.sixpay.common.time;

import java.time.Instant;

/**
 * Provides the current time without directly coupling application
 * components to the system clock.
 */
@FunctionalInterface
public interface TimeProvider {

    /**
     * Returns the current instant.
     *
     * @return current instant in UTC
     */
    Instant now();
}