package com.sixpay.bootstrap.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionBankingTransportGuardTest {

    private static MockEnvironment secureEnvironment() {
        return new MockEnvironment()
                .withProperty("SIXPAY_BANKING_MTLS_ENABLED", "true")
                .withProperty("sixpay.payment.banking.amplitude.reservation.base-url", "https://regional.test")
                .withProperty("sixpay.payment.banking.amplitude.posting.base-url", "https://regional.test")
                .withProperty("sixpay.payment.banking.amplitude.confirmation.base-url", "https://regional.test")
                .withProperty("sixpay.customer.verification.banking.base-url", "https://regional.test");
    }

    @Test
    void springProductionContextFailsAtStartupWithoutMtls() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("production");
            context.register(ProductionBankingTransportGuard.class);
            assertThrows(org.springframework.beans.factory.BeanCreationException.class, context::refresh);
        }
    }

    @Test
    void springNonProductionContextDoesNotInstallGuard() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.register(ProductionBankingTransportGuard.class);
            assertDoesNotThrow(context::refresh);
            org.junit.jupiter.api.Assertions.assertTrue(
                    context.getBeansOfType(ProductionBankingTransportGuard.class).isEmpty());
        }
    }

    @Test
    void validProductionConfigurationPasses() {
        assertDoesNotThrow(() -> new ProductionBankingTransportGuard(secureEnvironment()));
    }

    @Test
    void missingMtlsActivationFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(new MockEnvironment()));
    }

    @Test
    void explicitlyDisabledMtlsFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(
                        secureEnvironment().withProperty("SIXPAY_BANKING_MTLS_ENABLED", "false")));
    }

    @Test
    void paymentHttpFallbackFlagFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(secureEnvironment()
                        .withProperty("AMPLITUDE_POSTING_ALLOW_INSECURE_HTTP", "true")));
    }

    @Test
    void bankingHttpUrlFailsClosedEvenWhenLoopback() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(secureEnvironment()
                        .withProperty("sixpay.payment.banking.amplitude.posting.base-url",
                                "http://127.0.0.1:9443")));
    }

    @Test
    void customerHttpLoopbackFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(secureEnvironment()
                        .withProperty("sixpay.customer.verification.banking.base-url",
                                "http://localhost:9443")));
    }

    @Test
    void enabledAccountingHttpFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> new ProductionBankingTransportGuard(secureEnvironment()
                        .withProperty("sixpay.accounting.api.enabled", "true")
                        .withProperty("sixpay.accounting.api.base-url", "http://regional.test")));
    }

    @Test
    void disabledOptionalCapabilityDoesNotRequireUrl() {
        assertDoesNotThrow(() -> new ProductionBankingTransportGuard(secureEnvironment()
                .withProperty("sixpay.accounting.api.enabled", "false")));
    }
}
