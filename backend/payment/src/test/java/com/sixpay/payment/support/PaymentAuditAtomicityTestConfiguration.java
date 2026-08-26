package com.sixpay.payment.support;

import com.sixpay.payment.configuration.PaymentModuleConfiguration;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntity;
import com.sixpay.payment.infrastructure.audit.PaymentAuditRepository;
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
        basePackageClasses = PaymentAuditEntity.class
)
@EnableJpaRepositories(
        basePackageClasses = PaymentAuditRepository.class
)
@Import(PaymentAuditAdapter.class)
public class PaymentAuditAtomicityTestConfiguration {
}
