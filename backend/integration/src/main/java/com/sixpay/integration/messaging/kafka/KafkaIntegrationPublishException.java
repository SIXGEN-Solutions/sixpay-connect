package com.sixpay.integration.messaging.kafka;
import java.util.UUID;
public final class KafkaIntegrationPublishException extends RuntimeException {
    private final String topic;
    private final UUID eventId;
    public KafkaIntegrationPublishException(String topic, UUID eventId, Throwable cause) {
        super("Unable to publish integration event", cause);
        this.topic = topic;
        this.eventId = eventId;
    }
    public String topic() { return topic; }
    public UUID eventId() { return eventId; }
}
