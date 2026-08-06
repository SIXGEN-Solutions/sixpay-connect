package com.sixpay.integration.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import java.util.function.Supplier;

public final class IntegrationObservation {
    private final ObservationRegistry registry;
    public IntegrationObservation(ObservationRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }
    public <T> T observe(
            String integration,
            String provider,
            String operation,
            Supplier<T> action
    ) {
        return Observation.createNotStarted("sixpay.integration.operation", registry)
                .lowCardinalityKeyValue("integration", integration)
                .lowCardinalityKeyValue("provider", provider)
                .lowCardinalityKeyValue("operation", operation)
                .observe(action);
    }
}
