package com.sixpay.administration.configuration;

import com.sixpay.administration.AdministrationModule;
import com.sixpay.administration.infrastructure.persistence.DynamicSettingHistoryJpaEntity;
import com.sixpay.administration.infrastructure.persistence.DynamicSettingHistorySpringDataRepository;
import com.sixpay.administration.infrastructure.persistence.DynamicSettingJpaEntity;
import com.sixpay.administration.infrastructure.persistence.DynamicSettingSpringDataRepository;
import com.sixpay.administration.infrastructure.persistence.GeneralParameterJpaEntity;
import com.sixpay.administration.infrastructure.persistence.GeneralParameterSpringDataRepository;
import com.sixpay.administration.infrastructure.persistence.IncidentTimelineJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentSpringDataRepository;
import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({
        EntityManager.class,
        JpaRepository.class
})
@ComponentScan(
        basePackageClasses =
                AdministrationModule.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes =
                                TypeExcludeFilter.class
                ),
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes =
                                AutoConfigurationExcludeFilter.class
                )
        }
)
@EntityScan(
        basePackageClasses = {
                OperationalIncidentJpaEntity.class,
                IncidentTimelineJpaEntity.class,
                GeneralParameterJpaEntity.class,
                DynamicSettingJpaEntity.class,
                DynamicSettingHistoryJpaEntity.class
        }
)
@EnableJpaRepositories(
        basePackageClasses = {
                OperationalIncidentSpringDataRepository.class,
                GeneralParameterSpringDataRepository.class,
                DynamicSettingSpringDataRepository.class,
                DynamicSettingHistorySpringDataRepository.class
        }
)
public class AdministrationModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(TimeProvider.class)
    TimeProvider administrationTimeProvider() {
        return new SystemTimeProvider();
    }
}