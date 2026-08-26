package com.sixpay.integration.messaging.kafka;
import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
@FunctionalInterface
public interface KafkaEventRouter {
    KafkaEventRoute route(IntegrationEventEnvelope event);
}
