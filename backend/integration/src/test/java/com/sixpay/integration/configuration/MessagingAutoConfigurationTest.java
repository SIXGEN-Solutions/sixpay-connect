package com.sixpay.integration.configuration;

import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.messaging.internal.InternalEventBusTransport;
import com.sixpay.integration.messaging.kafka.KafkaEventTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            InternalMessagingAutoConfiguration.class,
                            KafkaMessagingAutoConfiguration.class
                    ));

    @Test
    void selectsInternalTransportByDefault() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(IntegrationEventTransport.class);
            assertThat(context.getBean(IntegrationEventTransport.class))
                    .isInstanceOf(InternalEventBusTransport.class);
        });
    }

    @Test
    void selectsKafkaOnlyWhenExplicitlyConfigured() {
        contextRunner
                .withUserConfiguration(KafkaTestConfiguration.class)
                .withPropertyValues(
                        "sixpay.messaging.transport=kafka"
                )
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(IntegrationEventTransport.class);
                    assertThat(context.getBean(
                            IntegrationEventTransport.class
                    )).isInstanceOf(KafkaEventTransport.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class KafkaTestConfiguration {

        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            @SuppressWarnings("unchecked")
            KafkaTemplate<String, String> template =
                    mock(KafkaTemplate.class);
            return template;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
