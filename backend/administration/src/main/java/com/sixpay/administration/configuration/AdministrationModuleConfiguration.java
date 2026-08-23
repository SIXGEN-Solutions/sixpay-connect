package com.sixpay.administration.configuration;

import com.sixpay.administration.infrastructure.persistence.IncidentTimelineJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentJpaEntity;
import com.sixpay.administration.infrastructure.persistence.OperationalIncidentSpringDataRepository;
import com.sixpay.common.time.SystemTimeProvider;
import com.sixpay.common.time.TimeProvider;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({
        EntityManager.class,
        JpaRepository.class
})
@EntityScan(
        basePackageClasses = {
                OperationalIncidentJpaEntity.class,
                IncidentTimelineJpaEntity.class
        }
)
@EnableJpaRepositories(
        basePackageClasses = {
                OperationalIncidentSpringDataRepository.class
        }
)
public class AdministrationModuleConfiguration {

    @Bean
    @ConditionalOnMissingBean(TimeProvider.class)
    TimeProvider administrationTimeProvider() {
        return new SystemTimeProvider();
    }
}
