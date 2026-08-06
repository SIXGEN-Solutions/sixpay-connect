package com.sixpay.integration.resilience;

import java.time.Duration;

@FunctionalInterface
public interface RetrySleeper {
    void sleep(Duration duration);
    static RetrySleeper threadSleep() {
        return duration -> {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Integration retry interrupted", e);
            }
        };
    }
}
