package com.sixpay.notification.configuration;

import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.notification.application.port.input.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.output.NotificationDeliveryStore;
import com.sixpay.notification.application.port.output.PartnerNotificationSender;
import com.sixpay.notification.application.port.output.PartnerStatusChangedEventDecoder;
import com.sixpay.notification.application.service.PartnerDecisionNotificationService;
import com.sixpay.notification.application.service.NotificationRetryPolicy;
import com.sixpay.notification.infrastructure.messaging.JacksonPartnerStatusChangedEventDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(before = NotificationMessagingAutoConfiguration.class)
public class NotificationApplicationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PartnerStatusChangedEventDecoder partnerStatusChangedEventDecoder(
            ObjectMapper objectMapper
    ) {
        return new JacksonPartnerStatusChangedEventDecoder(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    TimeProvider notificationTimeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    @ConditionalOnBean({
            PartnerNotificationSender.class,
            NotificationDeliveryStore.class
    })
    @ConditionalOnMissingBean(HandleIntegrationEventUseCase.class)
    HandleIntegrationEventUseCase handleIntegrationEventUseCase(
            PartnerStatusChangedEventDecoder decoder,
            PartnerNotificationSender sender,
            NotificationDeliveryStore deliveryStore,
            TimeProvider timeProvider,
            NotificationRetryPolicy retryPolicy
    ) {
        return new PartnerDecisionNotificationService(
                decoder,
                sender,
                deliveryStore,
                timeProvider,
                retryPolicy
        );
    }
}
