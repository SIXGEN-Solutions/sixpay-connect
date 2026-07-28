package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.in.HandleIntegrationEventUseCase;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.application.port.out.PartnerStatusChangedEventDecoder;
import com.sixpay.notification.application.service.PartnerDecisionNotificationService;
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
    @ConditionalOnBean(PartnerNotificationSender.class)
    @ConditionalOnMissingBean(HandleIntegrationEventUseCase.class)
    HandleIntegrationEventUseCase handleIntegrationEventUseCase(
            PartnerStatusChangedEventDecoder decoder,
            PartnerNotificationSender sender
    ) {
        return new PartnerDecisionNotificationService(decoder, sender);
    }
}
