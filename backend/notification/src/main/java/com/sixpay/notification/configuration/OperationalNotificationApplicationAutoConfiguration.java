package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.input.OperationalNotificationOrchestrationUseCase;
import com.sixpay.notification.application.port.input.OperationalNotificationTriggerUseCase;
import com.sixpay.notification.application.port.input.ProcessOperationalNotificationsUseCase;
import com.sixpay.notification.application.port.output.NotificationAttemptIdGenerator;
import com.sixpay.notification.application.port.output.NotificationIdGenerator;
import com.sixpay.notification.application.port.output.OperationalNotificationDeliveryGateway;
import com.sixpay.notification.application.port.output.SixPayAdminRecipientResolver;
import com.sixpay.notification.application.service.NotificationTemplateVariableMapper;
import com.sixpay.notification.application.service.OperationalNotificationDeliveryService;
import com.sixpay.notification.application.service.OperationalNotificationOrchestrationService;
import com.sixpay.notification.application.service.OperationalNotificationPlanningService;
import com.sixpay.notification.domain.policy.NotificationDeduplicationKeyFactory;
import com.sixpay.notification.domain.policy.NotificationDeliveryLifecycle;
import com.sixpay.notification.domain.policy.OperationalNotificationRetryPolicy;
import com.sixpay.notification.domain.policy.OperationalNotificationRoutingPolicy;
import com.sixpay.notification.domain.policy.OperationalNotificationTemplateCatalog;
import com.sixpay.notification.domain.repository.NotificationAttemptRepository;
import com.sixpay.notification.domain.repository.OperationalNotificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.UUID;

@AutoConfiguration(
        after = OperationalNotificationPersistenceAutoConfiguration.class
)
@EnableConfigurationProperties(
        OperationalNotificationRetryProperties.class
)
public class OperationalNotificationApplicationAutoConfiguration {

    @Bean("operationalNotificationClock")
    @ConditionalOnMissingBean(
            name = "operationalNotificationClock"
    )
    Clock operationalNotificationClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationIdGenerator operationalNotificationIdGenerator() {
        return UUID::randomUUID;
    }


    @Bean
    @ConditionalOnMissingBean
    NotificationAttemptIdGenerator
    operationalNotificationAttemptIdGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationDeliveryLifecycle
    operationalNotificationDeliveryLifecycle() {
        return new NotificationDeliveryLifecycle();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationDeduplicationKeyFactory
    operationalNotificationDeduplicationKeyFactory() {
        return new NotificationDeduplicationKeyFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    OperationalNotificationRoutingPolicy
    operationalNotificationRoutingPolicy() {
        return new OperationalNotificationRoutingPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    OperationalNotificationTemplateCatalog
    operationalNotificationTemplateCatalog() {
        return new OperationalNotificationTemplateCatalog();
    }

    @Bean
    @ConditionalOnMissingBean
    NotificationTemplateVariableMapper
    operationalNotificationTemplateVariableMapper() {
        return new NotificationTemplateVariableMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    OperationalNotificationRetryPolicy
    operationalNotificationRetryPolicy(
            OperationalNotificationRetryProperties properties
    ) {
        return new OperationalNotificationRetryPolicy(
                properties.maxAttempts(),
                properties.initialBackoff(),
                properties.maxBackoff()
        );
    }

    @Bean
    @ConditionalOnBean(
            SixPayAdminRecipientResolver.class
    )
    @ConditionalOnMissingBean(
            OperationalNotificationTriggerUseCase.class
    )
    OperationalNotificationTriggerUseCase
    operationalNotificationTriggerUseCase(
            SixPayAdminRecipientResolver recipientResolver,
            NotificationIdGenerator idGenerator,
            OperationalNotificationRoutingPolicy routingPolicy,
            NotificationDeduplicationKeyFactory deduplicationKeyFactory,
            NotificationTemplateVariableMapper variableMapper,
            OperationalNotificationTemplateCatalog templateCatalog,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationPlanningService(
                recipientResolver,
                idGenerator,
                routingPolicy,
                deduplicationKeyFactory,
                variableMapper,
                templateCatalog,
                clock
        );
    }

    @Bean
    @ConditionalOnBean({
            OperationalNotificationTriggerUseCase.class,
            OperationalNotificationRepository.class
    })
    @ConditionalOnMissingBean(
            OperationalNotificationOrchestrationUseCase.class
    )
    OperationalNotificationOrchestrationUseCase
    operationalNotificationOrchestrationUseCase(
            OperationalNotificationTriggerUseCase planner,
            OperationalNotificationRepository repository
    ) {
        return new OperationalNotificationOrchestrationService(
                planner,
                repository
        );
    }

    @Bean
    @ConditionalOnBean({
            OperationalNotificationRepository.class,
            NotificationAttemptRepository.class,
            OperationalNotificationDeliveryGateway.class
    })
    @ConditionalOnMissingBean(
            ProcessOperationalNotificationsUseCase.class
    )
    ProcessOperationalNotificationsUseCase
    processOperationalNotificationsUseCase(
            OperationalNotificationRepository repository,
            NotificationAttemptRepository attemptRepository,
            OperationalNotificationDeliveryGateway gateway,
            NotificationAttemptIdGenerator attemptIdGenerator,
            NotificationDeliveryLifecycle lifecycle,
            OperationalNotificationRetryPolicy retryPolicy,
            @Qualifier("operationalNotificationClock")
            Clock clock
    ) {
        return new OperationalNotificationDeliveryService(
                repository,
                attemptRepository,
                gateway,
                attemptIdGenerator,
                lifecycle,
                retryPolicy,
                clock
        );
    }
}
