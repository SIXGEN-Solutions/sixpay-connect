package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.input.HandleIntegrationEventUseCase;
import com.sixpay.notification.infrastructure.messaging.internal.InternalIntegrationEventListener;
import com.sixpay.notification.infrastructure.messaging.kafka.KafkaIntegrationEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            NotificationMessagingAutoConfiguration.class
                    ))
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void activatesOnlyTheInternalAdapterByDefault() {
        contextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(InternalIntegrationEventListener.class)
                    .doesNotHaveBean(KafkaIntegrationEventListener.class);
        });
    }

    @Test
    void activatesOnlyTheKafkaAdapterWhenSelected() {
        contextRunner
                .withPropertyValues(
                        "sixpay.messaging.transport=kafka"
                )
                .run(context -> {
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
    }
}
