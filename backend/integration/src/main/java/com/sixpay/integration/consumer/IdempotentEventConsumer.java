package com.sixpay.integration.consumer;

import com.sixpay.integration.event.DistributedEventEnvelope;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.util.Objects;

public final class IdempotentEventConsumer {

    private final String consumerName;
    private final ConsumedEventStore consumedEventStore;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public IdempotentEventConsumer(
            String consumerName,
            ConsumedEventStore consumedEventStore,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        if (consumerName == null
                || consumerName.isBlank()) {
            throw new IllegalArgumentException(
                    "consumerName is required"
            );
        }
        this.consumerName = consumerName.strip();
        this.consumedEventStore = Objects.requireNonNull(
                consumedEventStore
        );
        this.clock = Objects.requireNonNull(clock);
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry
        );
    }

    public void consume(
            DistributedEventEnvelope<?> event,
            DistributedEventHandler handler
    ) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(handler, "handler");

        boolean acquired = consumedEventStore.tryStart(
                consumerName,
                event.eventId(),
                clock.instant()
        );

        if (!acquired) {
            meterRegistry.counter(
                    "sixpay.integration.consumer.duplicates",
                    "consumer",
                    consumerName,
                    "eventType",
                    event.eventType()
            ).increment();
            return;
        }

        try {
            handler.handle(event);

            consumedEventStore.markCompleted(
                    consumerName,
                    event.eventId(),
                    clock.instant()
            );

            meterRegistry.counter(
                    "sixpay.integration.consumer.processed",
                    "consumer",
                    consumerName,
                    "eventType",
                    event.eventType()
            ).increment();
        } catch (RuntimeException exception) {
            consumedEventStore.markFailed(
                    consumerName,
                    event.eventId(),
                    clock.instant(),
                    "EVENT_HANDLER_FAILED"
            );

            meterRegistry.counter(
                    "sixpay.integration.consumer.errors",
                    "consumer",
                    consumerName,
                    "eventType",
                    event.eventType()
            ).increment();

            throw exception;
        }
    }
}
