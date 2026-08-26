package com.sixpay.integration.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;

public final class IntegrationMetrics {
    private final MeterRegistry registry;
    public IntegrationMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }
    public void recordRequest(
            String integration, String provider, String operation,
            String result, Duration duration
    ) {
        Counter.builder("sixpay.integration.requests")
                .tag("integration", safe(integration))
                .tag("provider", safe(provider))
                .tag("operation", safe(operation))
                .tag("result", safe(result))
                .register(registry).increment();
        Timer.builder("sixpay.integration.duration")
                .tag("integration", safe(integration))
                .tag("provider", safe(provider))
                .tag("operation", safe(operation))
                .tag("result", safe(result))
                .register(registry).record(duration);
    }
    public void recordRetry(String integration, String provider, String operation, int attempt) {
        Counter.builder("sixpay.integration.retries")
                .tag("integration", safe(integration))
                .tag("provider", safe(provider))
                .tag("operation", safe(operation))
                .tag("attempt", attempt >= 5 ? "5+" : Integer.toString(Math.max(1, attempt)))
                .register(registry).increment();
    }
    public void recordFailure(String integration, String provider, String operation, String category) {
        Counter.builder("sixpay.integration.failures")
                .tag("integration", safe(integration))
                .tag("provider", safe(provider))
                .tag("operation", safe(operation))
                .tag("category", safe(category))
                .register(registry).increment();
    }
    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
