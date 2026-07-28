package com.sixpay.notification.configuration;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.notification.application.port.in.RetryNotificationDeliveriesUseCase;
import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import com.sixpay.notification.application.port.out.PartnerNotificationSender;
import com.sixpay.notification.application.service.NotificationRetryPolicy;
import com.sixpay.notification.application.service.RetryNotificationDeliveriesService;
import com.sixpay.notification.infrastructure.scheduling.NotificationRetryScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(after = NotificationApplicationAutoConfiguration.class)
@EnableScheduling
public class NotificationRetryAutoConfiguration {

    @Bean
    @ConditionalOnBean({
            NotificationDeliveryStore.class,
            PartnerNotificationSender.class,
            TimeProvider.class
    })
    @ConditionalOnMissingBean(RetryNotificationDeliveriesUseCase.class)
    RetryNotificationDeliveriesUseCase retryNotificationDeliveriesUseCase(
            NotificationDeliveryStore deliveryStore,
            PartnerNotificationSender sender,
            TimeProvider timeProvider,
            NotificationRetryPolicy retryPolicy,
            NotificationRetryProperties properties
    ) {
        return new RetryNotificationDeliveriesService(
                deliveryStore,
                sender,
                timeProvider,
                retryPolicy,
                properties.getBatchSize()
        );
    }

    @Bean
    @ConditionalOnBean(RetryNotificationDeliveriesUseCase.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "sixpay.notification.retry",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    NotificationRetryScheduler notificationRetryScheduler(
            RetryNotificationDeliveriesUseCase retryUseCase
    ) {
        return new NotificationRetryScheduler(retryUseCase);
    }
}
