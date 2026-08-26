package com.sixpay.integration.configuration;

import com.sixpay.common.messaging.outbox.OutboxMessageSource;
import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.messaging.outbox.OutboxRelay;
import com.sixpay.integration.messaging.properties.OutboxRelayProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.List;

/**
 * Auto-configuration of the reliable Outbox relay.
 */
@AutoConfiguration(after = {
        InternalMessagingAutoConfiguration.class,
        KafkaMessagingAutoConfiguration.class
})
@ConditionalOnBean(IntegrationEventTransport.class)
@ConditionalOnProperty(
        prefix = "sixpay.messaging.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(OutboxRelayProperties.class)
@EnableScheduling
public class OutboxRelayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock outboxRelayClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxRelay outboxRelay(
            ObjectProvider<OutboxMessageSource> sources,
            IntegrationEventTransport transport,
            OutboxRelayProperties properties,
            Clock clock
    ) {
        List<OutboxMessageSource> orderedSources =
                sources.orderedStream().toList();

        return new OutboxRelay(
                orderedSources,
                transport,
                properties,
                clock
        );
    }
}
