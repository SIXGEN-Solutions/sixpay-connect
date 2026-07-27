package com.sixpay.integration.messaging.kafka;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;

/**
 * Resolves the Kafka topic without leaking broker naming into domains.
 */
@FunctionalInterface
public interface KafkaTopicResolver {

    String resolve(IntegrationEventEnvelope event);
}
