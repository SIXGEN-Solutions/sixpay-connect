package com.sixpay.payment.infrastructure.recovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "sixpay.payment.t0-recovery", name = "enabled", havingValue = "true")
public class PaymentT0RecoverySchedulingConfiguration {
}
