package com.sixpay.notification.configuration;

import com.sixpay.notification.application.service.NotificationRetryPolicy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = NotificationApplicationAutoConfiguration.class)
@EnableConfigurationProperties(NotificationRetryProperties.class)
public class NotificationRetryPolicyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    NotificationRetryPolicy notificationRetryPolicy(
            NotificationRetryProperties properties
    ) {
        return new NotificationRetryPolicy(
                properties.getMaxAttempts(),
                properties.getInitialDelay(),
                properties.getMultiplier(),
                properties.getMaxDelay()
        );
    }
}
