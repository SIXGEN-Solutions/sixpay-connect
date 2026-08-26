package com.sixpay.integration.kafka;

import java.util.Objects;

public record KafkaTopicRoute(
        String eventType,
        String topic
) {
    public KafkaTopicRoute {
        eventType = required(eventType, "eventType");
        topic = required(topic, "topic");
    }

    private static String required(
            String value,
            String name
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required"
            );
        }
        return value.strip();
    }
}
