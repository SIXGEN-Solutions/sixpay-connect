package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.input.ProcessOperationalNotificationsUseCase;
import com.sixpay.notification.infrastructure.operational.scheduling.OperationalNotificationRetryScheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;

@AutoConfiguration(
        after = OperationalNotificationApplicationAutoConfiguration.class
)
@EnableScheduling
public class OperationalNotificationRetryAutoConfiguration {

    @Bean
    @ConditionalOnBean(
            ProcessOperationalNotificationsUseCase.class
    )
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = OperationalNotificationRetryProperties.PREFIX,
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    OperationalNotificationRetryScheduler
    operationalNotificationRetryScheduler(
            ProcessOperationalNotificationsUseCase processor,
            OperationalNotificationRetryProperties properties,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationRetryScheduler(
                processor,
                properties,
                clock
        );
    }

    @Bean
    @ConditionalOnBean(
            OperationalNotificationRetryScheduler.class
    )
    OperationalNotificationRetryInvoker
    operationalNotificationRetryInvoker(
            OperationalNotificationRetryScheduler scheduler
    ) {
        return new OperationalNotificationRetryInvoker(
                scheduler
        );
    }

    static final class OperationalNotificationRetryInvoker {

        private final OperationalNotificationRetryScheduler scheduler;

        private OperationalNotificationRetryInvoker(
                OperationalNotificationRetryScheduler scheduler
        ) {
            this.scheduler = scheduler;
        }

        @Scheduled(
                fixedDelayString =
                        "${sixpay.notification.operational.retry.poll-interval-ms:30000}"
        )
        void run() {
            scheduler.runOnce();
        }
    }
}
