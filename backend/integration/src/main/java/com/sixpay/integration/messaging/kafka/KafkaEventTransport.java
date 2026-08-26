package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.common.validation.Preconditions;
import com.sixpay.integration.messaging.properties.KafkaMessagingProperties;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka transport prepared for the future microservices topology.
 */
public final class KafkaEventTransport
        implements IntegrationEventTransport {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicResolver topicResolver;
    private final KafkaMessagingProperties properties;

    public KafkaEventTransport(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopicResolver topicResolver,
            KafkaMessagingProperties properties
    ) {
        this.kafkaTemplate = Preconditions.requireNonNull(
                kafkaTemplate,
                "Kafka template must not be null"
        );
        this.objectMapper = Preconditions.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
        this.topicResolver = Preconditions.requireNonNull(
                topicResolver,
                "Kafka topic resolver must not be null"
        );
        this.properties = Preconditions.requireNonNull(
                properties,
                "Kafka messaging properties must not be null"
        );
    }

    @Override
    public void publish(IntegrationEventEnvelope event) {
        IntegrationEventEnvelope validatedEvent =
                Preconditions.requireNonNull(
                        event,
                        "Integration event must not be null"
                );

        try {
            String serializedEnvelope =
                    objectMapper.writeValueAsString(validatedEvent);

            kafkaTemplate.send(
                            topicResolver.resolve(validatedEvent),
                            validatedEvent.aggregateId().toString(),
                            serializedEnvelope
                    )
                    .get(
                            properties.publishTimeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Integration event serialization failed",
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Kafka publication was interrupted",
                    exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(
                    "Kafka publication was not confirmed",
                    exception
            );
        }
    }
}
