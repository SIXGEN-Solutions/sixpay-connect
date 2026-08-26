package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.integration.messaging.properties.KafkaMessagingProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultKafkaTopicResolverTest {

    @Test
    void buildsAVersionedTopicFromTheAggregateType() {
        DefaultKafkaTopicResolver resolver = new DefaultKafkaTopicResolver(
                new KafkaMessagingProperties(
                        "sixpay",
                        Duration.ofSeconds(5)
                )
        );

        String topic = resolver.resolve(new IntegrationEventEnvelope(
                UUID.randomUUID(),
                "PartnerCreatedIntegrationEvent",
                2,
                "PARTNER_ACCOUNT",
                UUID.randomUUID(),
                "correlation-1",
                Instant.parse("2026-07-27T10:00:00Z"),
                "{}"
        ));

        assertEquals("sixpay.partner-account.events.v2", topic);
    }
}
