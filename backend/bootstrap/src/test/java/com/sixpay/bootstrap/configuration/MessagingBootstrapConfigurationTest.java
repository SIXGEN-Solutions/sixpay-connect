package com.sixpay.bootstrap.configuration;

import com.sixpay.common.messaging.transport.IntegrationEventTransport;
import com.sixpay.integration.configuration.InternalMessagingAutoConfiguration;
import com.sixpay.integration.configuration.KafkaMessagingAutoConfiguration;
import com.sixpay.integration.messaging.internal.InternalEventBusTransport;
import com.sixpay.integration.messaging.kafka.KafkaEventTransport;
import com.sixpay.notification.application.port.input.HandleIntegrationEventUseCase;
import com.sixpay.notification.configuration.NotificationMessagingAutoConfiguration;
import com.sixpay.notification.infrastructure.messaging.internal.InternalIntegrationEventListener;
import com.sixpay.notification.infrastructure.messaging.kafka.KafkaIntegrationEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MessagingBootstrapConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            InternalMessagingAutoConfiguration.class,
                            KafkaMessagingAutoConfiguration.class,
                            NotificationMessagingAutoConfiguration.class
                    ))
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void wiresTheModularMonolithWithInternalMessaging() {
        contextRunner.run(context -> {
            assertThat(context.getBean(
                    IntegrationEventTransport.class
            )).isInstanceOf(InternalEventBusTransport.class);
            assertThat(context)
                    .hasSingleBean(InternalIntegrationEventListener.class)
                    .doesNotHaveBean(KafkaIntegrationEventListener.class);
        });
    }

    @Test
    void wiresTheDistributedModeWithKafkaMessaging() {
        contextRunner
                .withPropertyValues(
                        "sixpay.messaging.transport=kafka"
                )
                .run(context -> {
                    assertThat(context.getBean(
                            IntegrationEventTransport.class
                    )).isInstanceOf(KafkaEventTransport.class);
                    assertThat(context)
                            .hasSingleBean(KafkaIntegrationEventListener.class)
                            .doesNotHaveBean(
                                    InternalIntegrationEventListener.class
                            );
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean
        HandleIntegrationEventUseCase handleIntegrationEventUseCase() {
            return event -> {
            };
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            @SuppressWarnings("unchecked")
            KafkaTemplate<String, String> template =
                    mock(KafkaTemplate.class);
            return template;
        }
    }
}
