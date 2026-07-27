package com.sixpay.partner.configuration;

import com.sixpay.common.identifier.IdentifierGenerator;
import com.sixpay.common.identifier.UuidIdentifierGenerator;
import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import com.sixpay.partner.PartnerModule;
import com.sixpay.partner.infrastructure.audit.PartnerAuditJpaEntity;
import com.sixpay.partner.infrastructure.audit.PartnerAuditSpringDataRepository;
import com.sixpay.partner.infrastructure.audit.PartnerThresholdHistorySpringDataRepository;
import com.sixpay.partner.infrastructure.idempotency.PartnerIdempotencyJpaEntity;
import com.sixpay.partner.infrastructure.idempotency.PartnerIdempotencySpringDataRepository;
import com.sixpay.partner.infrastructure.outbox.OutboxEventJpaEntity;
import com.sixpay.partner.infrastructure.outbox.OutboxEventSpringDataRepository;
import com.sixpay.partner.infrastructure.persistence.PartnerJpaEntity;
import com.sixpay.partner.infrastructure.persistence.PartnerSpringDataRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.UUID;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@ComponentScan(basePackageClasses = PartnerModule.class)
@EntityScan(basePackageClasses = {
        PartnerJpaEntity.class,
        PartnerAuditJpaEntity.class,
        PartnerIdempotencyJpaEntity.class,
        OutboxEventJpaEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        PartnerSpringDataRepository.class,
        PartnerAuditSpringDataRepository.class,
        PartnerThresholdHistorySpringDataRepository.class,
        PartnerIdempotencySpringDataRepository.class,
        OutboxEventSpringDataRepository.class
})
public class PartnerModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean
    TimeProvider partnerTimeProvider() {
        return new SystemTimeProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    IdentifierGenerator<UUID> partnerIdentifierGenerator() {
        return new UuidIdentifierGenerator();
    }
}
