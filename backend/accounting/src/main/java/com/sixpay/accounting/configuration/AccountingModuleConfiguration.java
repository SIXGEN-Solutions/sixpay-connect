package com.sixpay.accounting.configuration;

import com.sixpay.accounting.AccountingModule;
import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.application.service.AccountingBatchBuilder;
import com.sixpay.accounting.application.service.AccountingBatchConstitutionService;
import com.sixpay.accounting.application.service.AccountingBatchIdempotencyKeyFactory;
import com.sixpay.accounting.application.service.AccountingBatchReconciliationService;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingEligibilityPolicy;
import com.sixpay.accounting.domain.policy.DailyAccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.VerifiedTresorPayStatusEligibilityPolicy;
import com.sixpay.accounting.domain.repository.AccountingBatchRepository;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import com.sixpay.accounting.domain.repository.AccountingReconciliationRepository;
import com.sixpay.accounting.infrastructure.persistence.AccountingBatchJpaEntity;
import com.sixpay.accounting.infrastructure.persistence.AccountingBatchSpringDataRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;

@AutoConfiguration
@ConditionalOnClass({
        EntityManager.class,
        JpaRepository.class
})
@EnableConfigurationProperties(
        AccountingBatchProperties.class
)
@ComponentScan(
        basePackageClasses = AccountingModule.class,
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
@EntityScan(
        basePackageClasses = AccountingBatchJpaEntity.class
)
@EnableJpaRepositories(
        basePackageClasses =
                AccountingBatchSpringDataRepository.class
)
public class AccountingModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock accountingClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    AccountingCutoffPolicy accountingCutoffPolicy(
            AccountingBatchProperties properties
    ) {
        return new DailyAccountingCutoffPolicy(
                properties.cutoffZone(),
                properties.cutoffTime()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    AccountingEligibilityPolicy
    accountingEligibilityPolicy() {
        return new VerifiedTresorPayStatusEligibilityPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    AccountingBatchIdempotencyKeyFactory
    accountingBatchIdempotencyKeyFactory() {
        return new AccountingBatchIdempotencyKeyFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    AccountingBatchBuilder accountingBatchBuilder(
            AccountingEligibilityPolicy eligibilityPolicy,
            AccountingBatchIdempotencyKeyFactory keyFactory,
            Clock accountingClock
    ) {
        return new AccountingBatchBuilder(
                eligibilityPolicy,
                keyFactory,
                accountingClock
        );
    }

    @Bean
    @ConditionalOnBean(
            PaymentAccountingCandidateSource.class
    )
    @ConditionalOnMissingBean
    AccountingBatchConstitutionService
    accountingBatchConstitutionService(
            AccountingCutoffPolicy cutoffPolicy,
            PaymentAccountingCandidateSource candidateSource,
            AccountingBatchBuilder batchBuilder,
            AccountingBatchRepository batchRepository
    ) {
        return new AccountingBatchConstitutionService(
                cutoffPolicy,
                candidateSource,
                batchBuilder,
                batchRepository
        );
    }

    @Bean
    @ConditionalOnBean(
            AccountingBatchGateway.class
    )
    @ConditionalOnMissingBean
    AccountingBatchReconciliationService
    accountingBatchReconciliationService(
            AccountingBatchRepository batchRepository,
            AccountingBatchTrackingRepository trackingRepository,
            AccountingReconciliationRepository reconciliationRepository,
            AccountingBatchGateway gateway,
            Clock accountingClock
    ) {
        return new AccountingBatchReconciliationService(
                batchRepository,
                trackingRepository,
                reconciliationRepository,
                gateway,
                accountingClock
        );
    }
}
