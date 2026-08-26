package com.sixpay.integration.messaging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Kafka-specific integration event settings.
 *
 * @param topicPrefix prefix applied to all SIXPAY event topics
 * @param publishTimeout maximum wait for broker acknowledgement
 */
@ConfigurationProperties(prefix = "sixpay.messaging.kafka")
public record KafkaMessagingProperties(
        String topicPrefix,
        Duration publishTimeout
) {

    public KafkaMessagingProperties {
        if (topicPrefix == null || topicPrefix.isBlank()) {
            topicPrefix = "sixpay";
        }
        if (publishTimeout == null) {
            publishTimeout = Duration.ofSeconds(5);
        }
        if (publishTimeout.isZero() || publishTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Kafka publish timeout must be positive"
            );
        }
    }
}
