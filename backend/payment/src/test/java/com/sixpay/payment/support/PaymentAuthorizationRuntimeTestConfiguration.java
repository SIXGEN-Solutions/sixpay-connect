package com.sixpay.payment.support;

import com.sixpay.payment.application.port.PaymentAuthorizationRuntimePort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

/**
 * Cross-module authorization dependencies for Payment-only infrastructure tests.
 * Unstubbed calls do not authorize any Payment (Mockito returns null).
 * Production wiring belongs to Bootstrap; this is test-only composition.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PaymentAuthorizationRuntimeTestConfiguration {

    @Bean
    PaymentAuthorizationRuntimePort paymentAuthorizationRuntimePort() {
        return mock(PaymentAuthorizationRuntimePort.class);
    }
}
