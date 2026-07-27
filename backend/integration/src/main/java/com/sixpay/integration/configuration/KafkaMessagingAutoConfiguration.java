package com.sixpay.integration.configuration;

import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.messaging.kafka.DefaultKafkaTopicResolver;
import com.sixpay.integration.messaging.kafka.KafkaEventTransport;
import com.sixpay.integration.messaging.kafka.KafkaTopicResolver;
import com.sixpay.integration.messaging.properties.KafkaMessagingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Kafka transport kept disabled until explicitly selected.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
        prefix = "sixpay.messaging",
        name = "transport",
        havingValue = "kafka"
)
@EnableConfigurationProperties(KafkaMessagingProperties.class)
public class KafkaMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    KafkaTopicResolver kafkaTopicResolver(
            KafkaMessagingProperties properties
    ) {
        return new DefaultKafkaTopicResolver(properties);
    }

    @Bean
    @ConditionalOnBean({
            KafkaTemplate.class,
            ObjectMapper.class
    })
    @ConditionalOnMissingBean(IntegrationEventTransport.class)
    IntegrationEventTransport kafkaIntegrationEventTransport(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            KafkaTopicResolver topicResolver,
            KafkaMessagingProperties properties
    ) {
        return new KafkaEventTransport(
                kafkaTemplate,
                objectMapper,
                topicResolver,
                properties
        );
    }
}
