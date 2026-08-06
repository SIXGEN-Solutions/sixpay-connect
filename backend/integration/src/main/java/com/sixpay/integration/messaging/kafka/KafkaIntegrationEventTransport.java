package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.messaging.json.IntegrationJsonSerializer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class KafkaIntegrationEventTransport implements IntegrationEventTransport {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventRouter router;
    private final IntegrationJsonSerializer serializer;
    public KafkaIntegrationEventTransport(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventRouter router,
            IntegrationJsonSerializer serializer
    ) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.router = Objects.requireNonNull(router);
        this.serializer = Objects.requireNonNull(serializer);
    }
    @Override
    public void publish(IntegrationEventEnvelope event) {
        Objects.requireNonNull(event, "event is required");
        KafkaEventRoute route = router.route(event);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(route.topic(), route.partitionKey(), serializer.serialize(event));
        header(record, KafkaIntegrationHeaders.EVENT_ID, event.eventId().toString());
        header(record, KafkaIntegrationHeaders.EVENT_TYPE, event.eventType());
        header(record, KafkaIntegrationHeaders.SCHEMA_VERSION, Integer.toString(event.schemaVersion()));
        header(record, KafkaIntegrationHeaders.CORRELATION_ID, event.correlationId());
        header(record, KafkaIntegrationHeaders.CONTENT_TYPE, "application/json");
        try {
            kafkaTemplate.send(record).join();
        } catch (RuntimeException e) {
            throw new KafkaIntegrationPublishException(route.topic(), event.eventId(), e);
        }
    }
    private static void header(ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
