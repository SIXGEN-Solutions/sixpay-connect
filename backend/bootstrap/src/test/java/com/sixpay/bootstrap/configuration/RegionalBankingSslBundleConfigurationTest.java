package com.sixpay.bootstrap.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionalBankingSslBundleConfigurationTest {
    @Test
    void disabledDoesNotRequireCertificatesOrRegisterBundle() {
        var registry = new DefaultSslBundleRegistry();
        var environment = new MockEnvironment()
                .withProperty("SIXPAY_BANKING_MTLS_ENABLED", "false");
        assertDoesNotThrow(() -> new RegionalBankingSslBundleConfiguration()
                .regionalBankingSslBundleRegistrar(environment, new DefaultResourceLoader())
                .registerBundles(registry));
        assertThrows(NoSuchSslBundleException.class,
                () -> registry.getBundle("amplitude-payment-client"));
    }

    @Test
    void enabledWithoutCertificatesFailsClosed() {
        var environment = new MockEnvironment()
                .withProperty("SIXPAY_BANKING_MTLS_ENABLED", "true");
        assertThrows(IllegalStateException.class,
                () -> new RegionalBankingSslBundleConfiguration()
                .regionalBankingSslBundleRegistrar(environment, new DefaultResourceLoader())
                .registerBundles(new DefaultSslBundleRegistry()));
    }
}
