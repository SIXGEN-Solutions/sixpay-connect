package com.sixpay.integration.outbox;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

public final class KafkaOutboxRelay {

    private final OutboxEventStore eventStore;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final OutboxTopicResolver topicResolver;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final int batchSize;
    private final String relayOwner;

    public KafkaOutboxRelay(
            OutboxEventStore eventStore,
            KafkaTemplate<String, byte[]> kafkaTemplate,
            OutboxTopicResolver topicResolver,
            MeterRegistry meterRegistry,
            Clock clock,
            int batchSize,
            String relayOwner
    ) {
        this.eventStore = Objects.requireNonNull(eventStore);
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate
        );
        this.topicResolver = Objects.requireNonNull(
                topicResolver
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry
        );
        this.clock = Objects.requireNonNull(clock);
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                    "batchSize must be >= 1"
            );
        }
        this.batchSize = batchSize;
        this.relayOwner = Objects.requireNonNull(
                relayOwner
        );
    }

    public int publishNextBatch() {
        List<OutboxEventRecord> records =
                eventStore.claimBatch(
                        batchSize,
                        clock.instant(),
                        relayOwner
                );

        int published = 0;

        for (OutboxEventRecord record : records) {
            String topic =
                    topicResolver.topicFor(
                            record.eventType()
                    );

            try {
                kafkaTemplate.send(
                                new ProducerRecord<>(
                                        topic,
                                        record.partitionKey(),
                                        record.payload()
                                )
                        )
                        .join();

                eventStore.markPublished(
                        record.outboxId(),
                        clock.instant()
                );

                meterRegistry.counter(
                        "sixpay.integration.outbox.published",
                        "topic",
                        topic,
                        "eventType",
                        record.eventType()
                ).increment();

                published++;
            } catch (RuntimeException exception) {
                eventStore.markFailed(
                        record.outboxId(),
                        clock.instant(),
                        "KAFKA_PUBLISH_FAILED"
                );

                meterRegistry.counter(
                        "sixpay.integration.outbox.publish.errors",
                        "topic",
                        topic,
                        "eventType",
                        record.eventType()
                ).increment();
            }
        }

        return published;
    }
}
