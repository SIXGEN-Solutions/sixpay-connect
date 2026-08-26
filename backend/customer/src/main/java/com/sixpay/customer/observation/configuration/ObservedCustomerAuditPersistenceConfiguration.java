package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import com.sixpay.customer.observation.infrastructure.audit.adapter
        .JpaObservedCustomerAuditAdapter;
import com.sixpay.customer.observation.infrastructure.audit.adapter
        .UuidObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.infrastructure.audit.entity
        .ObservedCustomerAuditJpaEntity;
import com.sixpay.customer.observation.infrastructure.audit.mapper
        .ObservedCustomerAuditPersistenceMapper;
import com.sixpay.customer.observation.infrastructure.audit.repository
        .ObservedCustomerAuditSpringDataRepository;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config
        .EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.audit.persistence",
        name = "enabled",
        havingValue = "true"
)
@EntityScan(
        basePackageClasses = {
                ObservedCustomerAuditJpaEntity.class
        }
)
@EnableJpaRepositories(
        basePackageClasses = {
                ObservedCustomerAuditSpringDataRepository.class
        }
)
public class ObservedCustomerAuditPersistenceConfiguration {

    @Bean
    ObservedCustomerAuditPersistenceMapper
    observedCustomerAuditPersistenceMapper() {

        return new ObservedCustomerAuditPersistenceMapper();
    }

    @Bean
    ObservedCustomerAuditIdGenerator
    observedCustomerAuditIdGenerator() {

        return new UuidObservedCustomerAuditIdGenerator();
    }

    @Bean
    ObservedCustomerAuditPort observedCustomerAuditPort(
            ObservedCustomerAuditSpringDataRepository repository,
            ObservedCustomerAuditPersistenceMapper mapper
    ) {
        return new JpaObservedCustomerAuditAdapter(
                repository,
                mapper
        );
    }
}