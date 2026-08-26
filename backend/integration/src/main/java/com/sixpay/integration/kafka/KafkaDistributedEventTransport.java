package com.sixpay.integration.kafka;

import com.sixpay.integration.event.DistributedEventEnvelope;
import com.sixpay.integration.event.DistributedEventSerializer;
import com.sixpay.integration.event.transport.DistributedEventTransport;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public final class KafkaDistributedEventTransport
        implements DistributedEventTransport {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final KafkaEventRouter router;
    private final DistributedEventSerializer serializer;
    private final MeterRegistry meterRegistry;

    public KafkaDistributedEventTransport(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            KafkaEventRouter router,
            DistributedEventSerializer serializer,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate
        );
        this.router = Objects.requireNonNull(router);
        this.serializer = Objects.requireNonNull(serializer);
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry
        );
    }

    @Override
    public CompletionStage<Void> publish(
            DistributedEventEnvelope<?> event
    ) {
        String topic = router.topicFor(event);

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(
                        topic,
                        event.partitionKey(),
                        serializer.serialize(event)
                );

        addHeader(
                record,
                "sixpay-event-id",
                event.eventId().toString()
        );
        addHeader(
                record,
                "sixpay-event-type",
                event.eventType()
        );
        addHeader(
                record,
                "sixpay-schema-version",
                Integer.toString(event.schemaVersion())
        );
        addHeader(
                record,
                "sixpay-correlation-id",
                event.correlationId()
        );
        if (event.causationId() != null) {
            addHeader(
                    record,
                    "sixpay-causation-id",
                    event.causationId()
            );
        }

        return kafkaTemplate.send(record)
                .thenAccept(result ->
                        meterRegistry.counter(
                                "sixpay.integration.kafka.published",
                                "topic",
                                topic,
                                "eventType",
                                event.eventType()
                        ).increment()
                )
                .exceptionally(exception -> {
                    meterRegistry.counter(
                            "sixpay.integration.kafka.publish.errors",
                            "topic",
                            topic,
                            "eventType",
                            event.eventType()
                    ).increment();
                    throw new KafkaEventPublicationException(
                            "Kafka publication failed",
                            exception
                    );
                });
    }

    private static void addHeader(
            ProducerRecord<String, byte[]> record,
            String name,
            String value
    ) {
        record.headers().add(
                new RecordHeader(
                        name,
                        value.getBytes(StandardCharsets.UTF_8)
                )
        );
    }
}
