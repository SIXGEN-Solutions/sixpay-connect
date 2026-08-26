package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import com.sixpay.common.validation.Preconditions;
import com.sixpay.integration.messaging.properties.KafkaMessagingProperties;

import java.util.Locale;

/**
 * Default versioned topic naming strategy.
 */
public final class DefaultKafkaTopicResolver
        implements KafkaTopicResolver {

    private final KafkaMessagingProperties properties;

    public DefaultKafkaTopicResolver(
            KafkaMessagingProperties properties
    ) {
        this.properties = Preconditions.requireNonNull(
                properties,
                "Kafka messaging properties must not be null"
        );
    }

    @Override
    public String resolve(IntegrationEventEnvelope event) {
        IntegrationEventEnvelope validatedEvent =
                Preconditions.requireNonNull(
                        event,
                        "Integration event must not be null"
                );

        String aggregate = validatedEvent.aggregateType()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');

        return properties.topicPrefix()
                + "."
                + aggregate
                + ".events.v"
                + validatedEvent.schemaVersion();
    }
}
