package com.sixpay.integration.configuration;

import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.messaging.internal.InternalEventBusTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * Internal Event Bus used by the modular monolith.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "sixpay.messaging",
        name = "transport",
        havingValue = "internal",
        matchIfMissing = true
)
public class InternalMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IntegrationEventTransport.class)
    IntegrationEventTransport internalIntegrationEventTransport(
            ApplicationEventPublisher eventPublisher
    ) {
        return new InternalEventBusTransport(eventPublisher);
    }
}
