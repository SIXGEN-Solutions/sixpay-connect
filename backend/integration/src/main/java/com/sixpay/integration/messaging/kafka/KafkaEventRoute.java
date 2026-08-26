package com.sixpay.integration.messaging.kafka;
public record KafkaEventRoute(String topic, String partitionKey) {
    public KafkaEventRoute {
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("topic is required");
        if (partitionKey == null || partitionKey.isBlank()) throw new IllegalArgumentException("partitionKey is required");
        topic = topic.strip();
        partitionKey = partitionKey.strip();
    }
}
