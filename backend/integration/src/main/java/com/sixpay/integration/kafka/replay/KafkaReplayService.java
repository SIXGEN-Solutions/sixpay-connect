package com.sixpay.integration.kafka.replay;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

public final class KafkaReplayService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final KafkaReplayAuditStore auditStore;
    private final Clock clock;

    public KafkaReplayService(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            KafkaReplayAuditStore auditStore,
            Clock clock
    ) {
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate
        );
        this.auditStore = Objects.requireNonNull(
                auditStore
        );
        this.clock = Objects.requireNonNull(clock);
    }

    public void replay(
            ReplayRequest request,
            String targetTopic,
            String partitionKey,
            byte[] originalPayload
    ) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(targetTopic);
        Objects.requireNonNull(partitionKey);
        Objects.requireNonNull(originalPayload);

        auditStore.recordRequested(request);

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(
                        targetTopic,
                        partitionKey,
                        originalPayload.clone()
                );

        addHeader(
                record,
                "sixpay-replay-id",
                request.replayId().toString()
        );
        addHeader(
                record,
                "sixpay-replayed-at",
                clock.instant().toString()
        );
        addHeader(
                record,
                "sixpay-replayed-by",
                request.requestedBy()
        );
        addHeader(
                record,
                "sixpay-replay-reason",
                request.reason()
        );
        addHeader(
                record,
                "sixpay-original-topic",
                request.sourceTopic()
        );
        addHeader(
                record,
                "sixpay-original-partition",
                Integer.toString(
                        request.sourcePartition()
                )
        );
        addHeader(
                record,
                "sixpay-original-offset",
                Long.toString(request.sourceOffset())
        );

        try {
            kafkaTemplate.send(record).join();
            auditStore.recordCompleted(request);
        } catch (RuntimeException exception) {
            auditStore.recordFailed(
                    request,
                    "KAFKA_REPLAY_FAILED"
            );
            throw exception;
        }
    }

    private static void addHeader(
            ProducerRecord<String, byte[]> record,
            String name,
            String value
    ) {
        record.headers().add(
                new RecordHeader(
                        name,
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );
    }
}
