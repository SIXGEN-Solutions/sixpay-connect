package com.sixpay.payment.support;

import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.infrastructure.outbox.PaymentDomainEventMapper;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(
        exclude = PaymentModuleConfiguration.class
)
@EntityScan(
        basePackageClasses = PaymentOutboxEntity.class
)
@EnableJpaRepositories(
        basePackageClasses = PaymentOutboxRepository.class
)
@Import(PaymentDomainEventMapper.class)
public class PaymentOutboxAtomicityTestConfiguration {
}
