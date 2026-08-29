package com.sixpay.customer.verification.infrastructure.banking.retry;

import java.time.Duration;

/**
 * Isolates retry waiting to keep the decorator deterministic input tests.
 */
@FunctionalInterface
public interface RetrySleeper {

    void sleep(Duration duration);

    static RetrySleeper threadSleep() {
        return duration -> {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Banking verification retry interrupted",
                        exception
                );
            }
        };
    }
}
