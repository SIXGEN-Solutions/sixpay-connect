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
            ObjectProvider<Clock> clockProvider
    ) {
        ObserveCustomerUseCase projectionService =
                new ObservedCustomerProjectionService(
                        customers,
                        payments,
                        idGenerator
                );

        ObservedCustomerAuditPort auditPort =
                auditPortProvider.getIfAvailable();

        ObservedCustomerAuditIdGenerator auditIdGenerator =
                auditIdGeneratorProvider.getIfAvailable();

        Clock clock =
                clockProvider.getIfAvailable();

        /*
         * Audit disabled:
         *
         * Preserve the historical projection behavior and still expose
         * ObserveCustomerUseCase.
         */
        if (auditPort == null
                && auditIdGenerator == null) {

            return transactional(
                    projectionService,
                    transactionManager,
                    properties
            );
        }

        /*
         * Partial audit wiring is invalid.
         *
         * If one audit dependency is present, all mandatory audit
         * dependencies must be available.
         */
        if (auditPort == null
                || auditIdGenerator == null
                || clock == null) {

            throw new IllegalStateException(
                    "Observed Customer projection audit "
                            + "configuration is incomplete"
            );
        }

        /*
         * Audit enabled:
         *
         * The successful audit is inside the projection transaction,
         * providing fail-closed behavior.
         */
        ObserveCustomerUseCase successAudited =
                new AuditedObserveCustomerUseCase(
                        projectionService,
                        auditPort,
                        auditIdGenerator,
                        clock
                );

        ObserveCustomerUseCase transactional =
                transactional(
                        successAudited,
                        transactionManager,
                        properties
                );

        /*
         * Final failure audit occurs after the projection transaction
         * has rolled back.
         */
        return new ProjectionFailureAuditingObserveCustomerUseCase(
                transactional,
                auditPort,
                auditIdGenerator,
                clock
        );
    }

    private static ObserveCustomerUseCase transactional(
            ObserveCustomerUseCase delegate,
            PlatformTransactionManager transactionManager,
            ObservedCustomerPersistenceProperties properties
    ) {
        return new TransactionalObserveCustomerUseCase(
                Objects.requireNonNull(
                        delegate,
                        "delegate is required"
                ),
                Objects.requireNonNull(
                        transactionManager,
                        "transactionManager is required"
                ),
                properties.maxOptimisticAttempts()
        );
    }
}