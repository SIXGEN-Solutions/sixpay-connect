package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.integration.messaging.properties.KafkaMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaEventTransportTest {

    @Test
    void publishesTheSerializedEnvelopeWithTheAggregateAsKey()
            throws Exception {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate =
                mock(KafkaTemplate.class);
        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        KafkaEventTransport transport = new KafkaEventTransport(
                kafkaTemplate,
                new ObjectMapper(),
                event -> "sixpay.partner.events.v1",
                new KafkaMessagingProperties(
                        "sixpay",
                        Duration.ofSeconds(1)
                )
        );
        IntegrationEventEnvelope event = event();

        transport.publish(event);

        verify(kafkaTemplate).send(
                "sixpay.partner.events.v1",
                event.aggregateId().toString(),
                new ObjectMapper().writeValueAsString(event)
        );
    }

    private static IntegrationEventEnvelope event() {
        return new IntegrationEventEnvelope(
                UUID.randomUUID(),
                "PartnerCreatedIntegrationEvent",
                1,
                "PARTNER",
                UUID.randomUUID(),
                "correlation-1",
                Instant.parse("2026-07-27T10:00:00Z"),
                "{\"partnerId\":\"123\"}"
        );
    }
}
