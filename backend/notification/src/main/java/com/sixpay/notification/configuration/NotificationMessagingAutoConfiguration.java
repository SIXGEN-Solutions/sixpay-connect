package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.infrastructure.messaging.internal.InternalIntegrationEventListener;
import com.sixpay.notification.infrastructure.messaging.kafka.KafkaIntegrationEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import tools.jackson.databind.ObjectMapper;

/**
 * Selects exactly one incoming messaging adapter for Notification.
 */
@AutoConfiguration
@ConditionalOnBean(HandleIntegrationEventUseCase.class)
public class NotificationMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "sixpay.messaging",
            name = "transport",
            havingValue = "internal",
            matchIfMissing = true
    )
    InternalIntegrationEventListener internalIntegrationEventListener(
            HandleIntegrationEventUseCase useCase
    ) {
        return new InternalIntegrationEventListener(useCase);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass({
            KafkaListener.class,
            ObjectMapper.class
    })
    @ConditionalOnProperty(
            prefix = "sixpay.messaging",
            name = "transport",
            havingValue = "kafka"
    )
    KafkaIntegrationEventListener kafkaIntegrationEventListener(
            HandleIntegrationEventUseCase useCase,
            ObjectMapper objectMapper
    ) {
        return new KafkaIntegrationEventListener(
                useCase,
                objectMapper
        );
    }
}
