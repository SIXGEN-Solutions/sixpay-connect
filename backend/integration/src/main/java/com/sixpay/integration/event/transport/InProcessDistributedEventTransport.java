package com.sixpay.integration.event.transport;

import com.sixpay.integration.event.DistributedEventEnvelope;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class InProcessDistributedEventTransport
        implements DistributedEventTransport {

    private final List<Consumer<DistributedEventEnvelope<?>>>
            consumers;

    public InProcessDistributedEventTransport(
            List<Consumer<DistributedEventEnvelope<?>>>
                    consumers
    ) {
        this.consumers = List.copyOf(
                Objects.requireNonNull(consumers)
        );
    }

    @Override
    public CompletionStage<Void> publish(
            DistributedEventEnvelope<?> event
    ) {
        Objects.requireNonNull(event, "event");

        for (Consumer<DistributedEventEnvelope<?>>
                consumer : consumers) {
            consumer.accept(event);
        }

        return CompletableFuture.completedFuture(null);
    }
}
