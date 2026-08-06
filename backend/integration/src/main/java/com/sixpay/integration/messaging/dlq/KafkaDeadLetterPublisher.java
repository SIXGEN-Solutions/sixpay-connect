package com.sixpay.integration.messaging.dlq;

import com.sixpay.integration.messaging.json.IntegrationJsonSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Objects;

public final class KafkaDeadLetterPublisher implements DeadLetterPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final IntegrationJsonSerializer serializer;
    public KafkaDeadLetterPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            IntegrationJsonSerializer serializer
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.serializer = Objects.requireNonNull(serializer);
    }
    @Override
    public void publish(DeadLetterRecord record) {
        Objects.requireNonNull(record, "record is required");
        try {
            kafkaTemplate.send(
                    record.destination(),
                    record.messageId().toString(),
                    serializer.serialize(record)
            ).join();
        } catch (RuntimeException e) {
            throw new DeadLetterPublicationException(e);
        }
    }
}
