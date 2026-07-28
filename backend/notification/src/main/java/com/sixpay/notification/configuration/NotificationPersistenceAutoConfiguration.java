package com.sixpay.notification.configuration;

import com.sixpay.notification.application.port.out.NotificationDeliveryStore;
import com.sixpay.notification.infrastructure.persistence.JpaNotificationDeliveryStore;
import com.sixpay.notification.infrastructure.persistence.NotificationDeliveryJpaEntity;
import com.sixpay.notification.infrastructure.persistence.NotificationDeliverySpringDataRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration(before = NotificationApplicationAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, JpaRepository.class})
@EntityScan(basePackageClasses = NotificationDeliveryJpaEntity.class)
@EnableJpaRepositories(
        basePackageClasses = NotificationDeliverySpringDataRepository.class
)
public class NotificationPersistenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    NotificationDeliveryStore notificationDeliveryStore(
            NotificationDeliverySpringDataRepository repository
    ) {
        return new JpaNotificationDeliveryStore(repository);
    }
}
