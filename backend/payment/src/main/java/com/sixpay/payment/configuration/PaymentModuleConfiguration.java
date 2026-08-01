package com.sixpay.payment.configuration;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.common.identifier.UuidIdentifierGenerator;
import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.PaymentModule;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntity;
import com.sixpay.payment.infrastructure.audit.PaymentAuditRepository;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import com.sixpay.payment.infrastructure.persistence.PaymentJpaEntity;
import com.sixpay.payment.infrastructure.persistence.PaymentSpringDataRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@ComponentScan(
        basePackageClasses = PaymentModule.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes = TypeExcludeFilter.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes = AutoConfigurationExcludeFilter.class
                )
        }
)
@EntityScan(basePackageClasses = {
        PaymentJpaEntity.class,
        PaymentAuditEntity.class,
        PaymentOutboxEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        PaymentSpringDataRepository.class,
        PaymentAuditRepository.class,
        PaymentOutboxRepository.class
})
public class PaymentModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TimeProvider paymentTimeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    IdentifierGenerator<UUID> paymentIdentifierGenerator() {
        return new UuidIdentifierGenerator();
    }
}
