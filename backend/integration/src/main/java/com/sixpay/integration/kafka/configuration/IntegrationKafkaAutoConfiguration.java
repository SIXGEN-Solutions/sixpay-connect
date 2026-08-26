package com.sixpay.integration.kafka.configuration;

import com.sixpay.integration.event.DistributedEventSerializer;
import com.sixpay.integration.event.JacksonDistributedEventSerializer;
import com.sixpay.integration.kafka.KafkaDistributedEventTransport;
import com.sixpay.integration.kafka.KafkaEventRouter;
import com.sixpay.integration.kafka.metrics.KafkaLagMetrics;
import com.sixpay.integration.event.transport.DistributedEventTransport;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(
        IntegrationKafkaProperties.class
)
@ConditionalOnProperty(
        prefix = IntegrationKafkaProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class IntegrationKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DistributedEventSerializer distributedEventSerializer(
            ObjectMapper objectMapper
    ) {
        return new JacksonDistributedEventSerializer(
                objectMapper
        );
    }

    @Bean
    KafkaEventRouter kafkaEventRouter(
            IntegrationKafkaProperties properties
    ) {
        return new KafkaEventRouter(
                properties.topics()
        );
    }

    @Bean
    DistributedEventTransport kafkaDistributedEventTransport(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            KafkaEventRouter router,
            DistributedEventSerializer serializer,
            MeterRegistry meterRegistry
    ) {
        return new KafkaDistributedEventTransport(
                kafkaTemplate,
                router,
                serializer,
                meterRegistry
        );
    }

    @Bean
    KafkaLagMetrics kafkaLagMetrics(
            MeterRegistry meterRegistry
    ) {
        return new KafkaLagMetrics(meterRegistry);
    }
}
