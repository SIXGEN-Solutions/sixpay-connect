package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerCursorCodec;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerPaymentQueryRepository;
import com.sixpay.customer.observation.application.port.output.query
        .ObservedCustomerQueryRepository;
import com.sixpay.customer.observation.application.service.query
        .ObservedCustomerQueryService;
import com.sixpay.customer.observation.infrastructure.persistence.protection
        .ObservedCustomerDataProtector;
import com.sixpay.customer.observation.infrastructure.query.adapter
        .JpaObservedCustomerPaymentQueryAdapter;
import com.sixpay.customer.observation.infrastructure.query.adapter
        .JpaObservedCustomerQueryAdapter;
import com.sixpay.customer.observation.infrastructure.query.cursor
        .HmacObservedCustomerCursorCodec;
import com.sixpay.customer.observation.infrastructure.query.mapper
        .ObservedCustomerQueryRowMapper;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.context.properties
        .EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        ObservedCustomerQueryProperties.class
)
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.query",
        name = "enabled",
        havingValue = "true"
)
public class ObservedCustomerQueryConfiguration {

    @Bean
    ObservedCustomerQueryRowMapper
    observedCustomerQueryRowMapper(
            ObservedCustomerDataProtector protector
    ) {
        return new ObservedCustomerQueryRowMapper(
                protector
        );
    }

    @Bean
    ObservedCustomerCursorCodec
    observedCustomerCursorCodec(
            ObservedCustomerQueryProperties properties
    ) {
        return new HmacObservedCustomerCursorCodec(
                properties.decodedCursorKey()
        );
    }

    @Bean
    ObservedCustomerQueryRepository
    observedCustomerQueryRepository(
            EntityManager entityManager,
            ObservedCustomerDataProtector protector,
            ObservedCustomerQueryRowMapper mapper
    ) {
        return new JpaObservedCustomerQueryAdapter(
                entityManager,
                protector,
                mapper
        );
    }

    @Bean
    ObservedCustomerPaymentQueryRepository
    observedCustomerPaymentQueryRepository(
            EntityManager entityManager,
            ObservedCustomerQueryRowMapper mapper
    ) {
        return new JpaObservedCustomerPaymentQueryAdapter(
                entityManager,
                mapper
        );
    }

    @Bean
    ObservedCustomerQueryService
    observedCustomerQueryService(
            ObservedCustomerQueryRepository customers,
            ObservedCustomerPaymentQueryRepository payments,
            ObservedCustomerCursorCodec cursorCodec
    ) {
        return new ObservedCustomerQueryService(
                customers,
                payments,
                cursorCodec
        );
    }
}