package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.input.OperationalNotificationOperationsUseCase;
import com.sixpay.notification.application.port.input.OperationalNotificationRetentionUseCase;
import com.sixpay.notification.application.port.output.NotificationReplayIdGenerator;
import com.sixpay.notification.application.port.output.OperationalNotificationOperationsTelemetry;
import com.sixpay.notification.application.service.OperationalNotificationOperationsService;
import com.sixpay.notification.application.service.OperationalNotificationRetentionService;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.NotificationReplayRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationOperationsRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
import com.sixpay.notification.infrastructure.operational.observability.OperationalNotificationMetrics;
import com.sixpay.notification.infrastructure.operational.observability.OperationalNotificationMetricsRefresher;
import com.sixpay.notification.infrastructure.operational.scheduling.OperationalNotificationRetentionScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.UUID;

@AutoConfiguration(
        after = OperationalNotificationApplicationAutoConfiguration.class
)
@EnableScheduling
@EnableConfigurationProperties(
        OperationalNotificationOperationsProperties.class
)
public class OperationalNotificationOperationsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    NotificationReplayIdGenerator
    operationalNotificationReplayIdGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnProperty(
            prefix = OperationalNotificationOperationsProperties.PREFIX,
            name = "metrics-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(
            OperationalNotificationOperationsTelemetry.class
    )
    OperationalNotificationMetrics
    operationalNotificationMetrics(
            MeterRegistry registry
    ) {
        return new OperationalNotificationMetrics(
                registry
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            OperationalNotificationOperationsTelemetry.class
    )
    OperationalNotificationOperationsTelemetry
    operationalNotificationOperationsNoopTelemetry() {
        return OperationalNotificationOperationsTelemetry.NOOP;
    }

    @Bean
    @ConditionalOnBean({
            OperationalNotificationRepository.class,
            OperationalNotificationOperationsRepository.class,
            NotificationAttemptRepository.class,
            NotificationReplayRepository.class
    })
    @ConditionalOnMissingBean(
            OperationalNotificationOperationsUseCase.class
    )
    OperationalNotificationOperationsUseCase
    operationalNotificationOperationsUseCase(
            OperationalNotificationRepository repository,
            OperationalNotificationOperationsRepository operationsRepository,
            NotificationAttemptRepository attemptRepository,
            NotificationReplayRepository replayRepository,
            NotificationReplayIdGenerator replayIdGenerator,
            OperationalNotificationOperationsTelemetry telemetry,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationOperationsService(
                repository,
                operationsRepository,
                attemptRepository,
                replayRepository,
                replayIdGenerator,
                telemetry,
                clock
        );
    }

    @Bean
    @ConditionalOnBean(
            OperationalNotificationOperationsRepository.class
    )
    @ConditionalOnMissingBean(
            OperationalNotificationRetentionUseCase.class
    )
    OperationalNotificationRetentionUseCase
    operationalNotificationRetentionUseCase(
            OperationalNotificationOperationsRepository repository,
            OperationalNotificationOperationsProperties properties,
            OperationalNotificationOperationsTelemetry telemetry
    ) {
        return new OperationalNotificationRetentionService(
                repository,
                properties.deliveredRetention(),
                properties.failedRetention(),
                properties.purgeBatchSize(),
                telemetry
        );
    }

    @Bean
    @ConditionalOnBean({
            OperationalNotificationOperationsRepository.class,
            OperationalNotificationMetrics.class
    })
    OperationalNotificationMetricsRefresher
    operationalNotificationMetricsRefresher(
            OperationalNotificationOperationsRepository repository,
            OperationalNotificationMetrics metrics,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationMetricsRefresher(
                repository,
                metrics,
                clock
        );
    }

    @Bean
    @ConditionalOnBean(
            OperationalNotificationRetentionUseCase.class
    )
    @ConditionalOnProperty(
            prefix = OperationalNotificationOperationsProperties.PREFIX,
            name = "retention-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    OperationalNotificationRetentionScheduler
    operationalNotificationRetentionScheduler(
            OperationalNotificationRetentionUseCase retention,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationRetentionScheduler(
                retention,
                clock
        );
    }

    @Bean
    @ConditionalOnBean(
            OperationalNotificationMetricsRefresher.class
    )
    MetricsInvoker operationalNotificationMetricsInvoker(
            OperationalNotificationMetricsRefresher refresher
    ) {
        return new MetricsInvoker(
                refresher
        );
    }

    @Bean
    @ConditionalOnBean(
            OperationalNotificationRetentionScheduler.class
    )
    RetentionInvoker operationalNotificationRetentionInvoker(
            OperationalNotificationRetentionScheduler scheduler
    ) {
        return new RetentionInvoker(
                scheduler
        );
    }

    static final class MetricsInvoker {

        private final OperationalNotificationMetricsRefresher refresher;

        private MetricsInvoker(
                OperationalNotificationMetricsRefresher refresher
        ) {
            this.refresher = refresher;
        }

        @Scheduled(
                fixedDelayString =
                        "${sixpay.notification.operational.operations.metrics-refresh-ms:30000}"
        )
        void refresh() {
            refresher.refresh();
        }
    }

    static final class RetentionInvoker {

        private final OperationalNotificationRetentionScheduler scheduler;

        private RetentionInvoker(
                OperationalNotificationRetentionScheduler scheduler
        ) {
            this.scheduler = scheduler;
        }

        @Scheduled(
                fixedDelayString =
                        "${sixpay.notification.operational.operations.purge-interval-ms:86400000}"
        )
        void purge() {
            scheduler.runOnce();
        }
    }
}
