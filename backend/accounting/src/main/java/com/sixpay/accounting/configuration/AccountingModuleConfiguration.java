package com.sixpay.accounting.configuration;

import com.sixpay.accounting.AccountingModule;
import com.sixpay.accounting.application.port.output.AccountingBatchGateway;
import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.PaymentAccountingCandidateSource;
import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.application.service.AccountingBatchBuilder;
import com.sixpay.accounting.application.service.AccountingT1OrchestrationService;
import com.sixpay.accounting.application.service.AccountingT1ManualExecutionService;
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
import com.sixpay.accounting.infrastructure.tfj.persistence.AccountingTfjConfirmationJpaEntity;
import com.sixpay.accounting.infrastructure.tfj.persistence.AccountingTfjConfirmationRepositoryAdapter;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
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
        basePackageClasses = {
                AccountingBatchJpaEntity.class,
                AccountingTfjConfirmationJpaEntity.class
        }
)
@EnableJpaRepositories(
        basePackageClasses = {
                AccountingBatchSpringDataRepository.class,
                AccountingTfjConfirmationRepositoryAdapter.class
        }
)
public class AccountingModuleConfiguration {

    public static final String ACCOUNTING_CLOCK =
            "accountingClock";

    @Bean(name = ACCOUNTING_CLOCK)
    @ConditionalOnMissingBean(
            name = ACCOUNTING_CLOCK
    )
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
            @Qualifier(ACCOUNTING_CLOCK)
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
            AccountingCandidateProjectionRepository.class
    )
    @ConditionalOnMissingBean
    AccountingBatchConstitutionService
    accountingBatchConstitutionService(
            AccountingCutoffPolicy cutoffPolicy,
            AccountingCandidateProjectionRepository projectionRepository,
            AccountingBatchBuilder batchBuilder,
            AccountingBatchRepository batchRepository
    ) {
        return new AccountingBatchConstitutionService(
                cutoffPolicy,
                projectionRepository,
                batchBuilder,
                batchRepository
        );
    }

    @Bean
    @ConditionalOnBean({
            AccountingCandidateProjectionRepository.class,
            TresorPayPaymentStatusGateway.class,
            AccountingBatchConstitutionService.class
    })
    @ConditionalOnMissingBean
    AccountingT1OrchestrationService accountingT1OrchestrationService(
            AccountingCutoffPolicy cutoffPolicy,
            AccountingCandidateProjectionRepository projectionRepository,
            TresorPayPaymentStatusGateway tresorPayGateway,
            AccountingBatchConstitutionService constitutionService
    ) {
        return new AccountingT1OrchestrationService(
                cutoffPolicy,
                projectionRepository,
                tresorPayGateway,
                constitutionService
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
            @Qualifier(ACCOUNTING_CLOCK)
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
    @Bean
    @ConditionalOnBean({
            AccountingCandidateProjectionRepository.class,
            AccountingBatchQueryPort.class,
            AccountingT1OrchestrationService.class,
            AccountingBatchReconciliationService.class,
            AccountingBatchTrackingRepository.class
    })
    @ConditionalOnMissingBean
    AccountingT1ManualExecutionService accountingT1ManualExecutionService(
            @Qualifier(ACCOUNTING_CLOCK) Clock accountingClock,
            AccountingCutoffPolicy cutoffPolicy,
            AccountingCandidateProjectionRepository projectionRepository,
            AccountingBatchQueryPort queryPort,
            AccountingT1OrchestrationService orchestrationService,
            AccountingBatchReconciliationService reconciliationService,
            AccountingBatchTrackingRepository trackingRepository
    ) {
        return new AccountingT1ManualExecutionService(
                accountingClock,
                cutoffPolicy,
                projectionRepository,
                queryPort,
                orchestrationService,
                reconciliationService,
                trackingRepository
        );
    }

}
