package com.sixpay.notification.infrastructure.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.validation.Preconditions;
import com.sixpay.notification.application.port.input.HandleIntegrationEventUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka incoming adapter prepared for the distributed topology.
 */
public final class KafkaIntegrationEventListener {

    private final HandleIntegrationEventUseCase useCase;
    private final ObjectMapper objectMapper;

    public KafkaIntegrationEventListener(
            HandleIntegrationEventUseCase useCase,
            ObjectMapper objectMapper
    ) {
        this.useCase = Preconditions.requireNonNull(
                useCase,
                "Integration event use case must not be null"
        );
        this.objectMapper = Preconditions.requireNonNull(
                objectMapper,
                "Object mapper must not be null"
        );
    }

    @KafkaListener(
            topicPattern = "${sixpay.messaging.kafka.notification-topic-pattern:"
                    + "sixpay\\..*\\.events\\.v[0-9]+}",
            groupId = "${sixpay.messaging.kafka.notification-group-id:"
                    + "sixpay-notification}"
    )
    public void onIntegrationEvent(String serializedEnvelope) {
        String payload = Preconditions.requireNonBlank(
                serializedEnvelope,
                "Serialized integration event must not be blank"
        );

        try {
            useCase.handle(
                    objectMapper.readValue(
                            payload,
                            IntegrationEventEnvelope.class
                    )
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Invalid integration event envelope",
                    exception
            );
        }
    }
}
