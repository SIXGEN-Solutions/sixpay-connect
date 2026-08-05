package com.sixpay.customer.observation.infrastructure.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/**
 * Infrastructure backoff implementation. It does not use Thread.sleep().
 */
public final class LockSupportObservedCustomerProjectionBackoff
        implements ObservedCustomerProjectionBackoff {

    @Override
    public void pause(Duration delay) {
        Objects.requireNonNull(delay, "delay is required");

        if (delay.isNegative()) {
            throw new IllegalArgumentException(
                    "delay must not be negative"
            );
        }

        if (delay.isZero()) {
            return;
        }

        LockSupport.parkNanos(delay.toNanos());

        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException(
                    "Observed Customer projection retry interrupted"
            );
        }
    }
}
