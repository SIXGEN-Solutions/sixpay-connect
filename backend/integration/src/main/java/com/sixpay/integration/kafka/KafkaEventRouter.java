package com.sixpay.integration.kafka;

import com.sixpay.integration.event.DistributedEventEnvelope;

import java.util.Map;
import java.util.Objects;

public final class KafkaEventRouter {

    private final Map<String, String> topicByEventType;

    public KafkaEventRouter(
            Map<String, String> topicByEventType
    ) {
        this.topicByEventType = Map.copyOf(
                Objects.requireNonNull(topicByEventType)
        );
    }

    public String topicFor(
            DistributedEventEnvelope<?> event
    ) {
        String topic = topicByEventType.get(
                event.eventType()
        );

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException(
                    "No Kafka topic configured for "
                            + event.eventType()
            );
        }

        return topic;
    }
}
