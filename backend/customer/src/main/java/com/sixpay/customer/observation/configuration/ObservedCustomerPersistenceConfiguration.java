package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.application.port.output
        .ObservedCustomerIdGenerator;
import com.sixpay.customer.observation.application.port.output
        .ObservedCustomerRepository;
import com.sixpay.customer.observation.application.port.output
        .ObservedPaymentRepository;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import com.sixpay.customer.observation.application.service
        .ObservedCustomerProjectionService;
import com.sixpay.customer.observation.application.service.audit
        .AuditedObserveCustomerUseCase;
import com.sixpay.customer.observation.application.service.audit
        .ProjectionFailureAuditingObserveCustomerUseCase;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionMetrics;
import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionObservation;
import com.sixpay.customer.observation.infrastructure.persistence.adapter
        .JpaObservedCustomerRepositoryAdapter;
import com.sixpay.customer.observation.infrastructure.persistence.adapter
        .JpaObservedPaymentRepositoryAdapter;
import com.sixpay.customer.observation.infrastructure.persistence.adapter
        .UuidObservedCustomerIdGenerator;
import com.sixpay.customer.observation.infrastructure.persistence.mapper
        .ObservedCustomerPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.persistence.protection
        .AesGcmObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.persistence.protection
        .ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.persistence.repository
        .ObservedCustomerSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository
        .ObservedPaymentSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.repository
        .ProcessedObservationEventSpringDataRepository;
import com.sixpay.customer.observation.infrastructure.persistence.transaction
        .TransactionalObserveCustomerUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.context.properties
        .EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        ObservedCustomerPersistenceProperties.class
)
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.persistence",
        name = "enabled",
        havingValue = "true"
)
public class ObservedCustomerPersistenceConfiguration {

    @Bean
    ObservedCustomerDataProtector observedCustomerDataProtector(
            ObservedCustomerPersistenceProperties properties
    ) {
        return new AesGcmObservedCustomerDataProtector(
                properties.protectionKeyBase64()
        );
    }

    @Bean
    ObservedCustomerPersistenceMapper
    observedCustomerPersistenceMapper(
            ObservedCustomerDataProtector protector
    ) {
        return new ObservedCustomerPersistenceMapper(
                protector
        );
    }

    @Bean
    ObservedCustomerRepository observedCustomerRepository(
            ObservedCustomerSpringDataRepository customers,
            ObservedPaymentSpringDataRepository payments,
            ProcessedObservationEventSpringDataRepository events,
            ObservedCustomerDataProtector protector,
            ObservedCustomerPersistenceMapper mapper
    ) {
        return new JpaObservedCustomerRepositoryAdapter(
                customers,
                payments,
                events,
                protector,
                mapper
        );
    }

    @Bean
    ObservedPaymentRepository observedPaymentRepository(
            ObservedCustomerSpringDataRepository customers,
            ObservedPaymentSpringDataRepository payments,
            ProcessedObservationEventSpringDataRepository events,
            ObservedCustomerPersistenceMapper mapper
    ) {
        return new JpaObservedPaymentRepositoryAdapter(
                customers,
                payments,
                events,
                mapper
        );
    }

    @Bean
    ObservedCustomerIdGenerator observedCustomerIdGenerator() {
        return new UuidObservedCustomerIdGenerator();
    }

    @Bean
    ObserveCustomerUseCase observeCustomerUseCase(
            ObservedCustomerRepository customers,
            ObservedPaymentRepository payments,
            ObservedCustomerIdGenerator idGenerator,
            PlatformTransactionManager transactionManager,
            ObservedCustomerPersistenceProperties properties,
            ObjectProvider<ObservedCustomerAuditPort>
                    auditPortProvider,
            ObjectProvider<ObservedCustomerAuditIdGenerator>
                    auditIdGeneratorProvider,
            ObjectProvider<ObservedCustomerProjectionMetrics>
                    metricsProvider,
            ObjectProvider<Clock> clockProvider
    ) {
        ObserveCustomerUseCase projection =
                new ObservedCustomerProjectionService(
                        customers,
                        payments,
                        idGenerator
                );

        ObservedCustomerAuditPort auditPort =
                auditPortProvider.getIfAvailable();
        ObservedCustomerAuditIdGenerator auditIdGenerator =
                auditIdGeneratorProvider.getIfAvailable();
        ObservedCustomerProjectionMetrics metrics =
                metricsProvider.getIfAvailable();
        Clock clock =
                clockProvider.getIfAvailable();

        if ((auditPort == null)
                != (auditIdGenerator == null)) {
            throw new IllegalStateException(
                    "Observed Customer projection audit "
                            + "configuration is incomplete"
            );
        }

        if ((auditPort != null || metrics != null)
                && clock == null) {
            throw new IllegalStateException(
                    "Observed Customer projection Clock "
                            + "is required"
            );
        }

        ObserveCustomerUseCase transactionalDelegate =
                projection;

        if (auditPort != null) {
            transactionalDelegate =
                    new AuditedObserveCustomerUseCase(
                            projection,
                            auditPort,
                            auditIdGenerator,
                            clock
                    );
        }

        ObserveCustomerUseCase transactional =
                new TransactionalObserveCustomerUseCase(
                        transactionalDelegate,
                        Objects.requireNonNull(
                                transactionManager,
                                "transactionManager is required"
                        ),
                        properties.maxOptimisticAttempts(),
                        metrics
                );

        ObserveCustomerUseCase finalDelegate =
                transactional;

        if (auditPort != null) {
            finalDelegate =
                    new ProjectionFailureAuditingObserveCustomerUseCase(
                            transactional,
                            auditPort,
                            auditIdGenerator,
                            clock
                    );
        }

        if (metrics != null) {
            finalDelegate =
                    new ObservedCustomerProjectionObservation(
                            finalDelegate,
                            metrics,
                            clock
                    );
        }

        return finalDelegate;
    }
}
