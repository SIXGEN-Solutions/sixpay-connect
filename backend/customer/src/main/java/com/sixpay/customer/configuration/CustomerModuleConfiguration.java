package com.sixpay.customer.configuration;

import com.sixpay.customer.CustomerModule;
import com.sixpay.customer.management.infrastructure.audit.CustomerAuditJpaEntity;
import com.sixpay.customer.management.infrastructure.audit.CustomerAuditSpringDataRepository;
import com.sixpay.customer.management.infrastructure.persistence.CustomerJpaEntity;
import com.sixpay.customer.management.infrastructure.persistence.CustomerSpringDataRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot auto-configuration entry point for the Customer module.
 *
 * <p>The module owns Customer Verification, Observed Customer and
 * Customer Management capabilities.</p>
 *
 * <p>Optional banking infrastructure is activated by its own conditional
 * configuration and is not required by unrelated module tests.</p>
 */
@AutoConfiguration
@ConditionalOnClass({
        EntityManager.class,
        JpaRepository.class
})
@ComponentScan(
        basePackageClasses = CustomerModule.class,
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
                CustomerJpaEntity.class,
                CustomerAuditJpaEntity.class
        }
)
@EnableJpaRepositories(
        basePackageClasses = {
                CustomerSpringDataRepository.class,
                CustomerAuditSpringDataRepository.class
        }
)
public class CustomerModuleConfiguration {
}