package com.sixpay.integration.outbox;

import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class OutboxCleanupService {

    private final OutboxEventStore eventStore;
    private final Clock clock;
    private final Duration retention;
    private final int batchSize;
    private final MeterRegistry meterRegistry;

    public OutboxCleanupService(
            OutboxEventStore eventStore,
            Clock clock,
            Duration retention,
            int batchSize,
            MeterRegistry meterRegistry
    ) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.clock = Objects.requireNonNull(clock);
        this.retention = Objects.requireNonNull(retention);
        if (retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException(
                    "retention must be positive"
            );
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be >= 1"
            );
        }
        this.batchSize = batchSize;
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry
        );
    }

    public int cleanup() {
        int deleted = eventStore.deletePublishedBefore(
                clock.instant().minus(retention),
                batchSize
        );

        meterRegistry.counter(
                "sixpay.integration.outbox.cleaned"
        ).increment(deleted);

        return deleted;
    }
}
