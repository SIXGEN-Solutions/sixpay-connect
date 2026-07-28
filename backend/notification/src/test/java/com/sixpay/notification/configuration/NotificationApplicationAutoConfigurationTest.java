package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.application.service.PartnerDecisionNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationApplicationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            NotificationApplicationAutoConfiguration.class
                    ))
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void activatesBusinessHandlerWhenSenderAdapterIsAvailable() {
        contextRunner
                .withUserConfiguration(SenderConfiguration.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(HandleIntegrationEventUseCase.class)
                        .hasSingleBean(PartnerDecisionNotificationService.class));
    }

    @Test
    void doesNotPretendToNotifyWithoutSenderAdapter() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(HandleIntegrationEventUseCase.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class SenderConfiguration {

        @Bean
        PartnerNotificationSender partnerNotificationSender() {
            return ignored -> {
            };
        }
    }
}
