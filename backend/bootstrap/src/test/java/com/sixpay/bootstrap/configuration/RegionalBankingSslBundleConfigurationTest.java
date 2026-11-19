package com.sixpay.bootstrap.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class RegionalBankingSslBundleConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            RegionalBankingSslBundleConfiguration.class
                    );

    @Test
    void disabledDoesNotRequireCertificatesOrRegisterBundle() {
        runner.withPropertyValues(
                "SIXPAY_BANKING_MTLS_ENABLED=false"
        ).run(context -> {

            assertNull(context.getStartupFailure());

            assertFalse(context.containsBean(
                    "regionalBankingSslBundleRegistrar"
            ));

            assertTrue(context.getBeansOfType(
                    SslBundleRegistrar.class
            ).isEmpty());

            var registry = new DefaultSslBundleRegistry();

            assertThrows(
                    NoSuchSslBundleException.class,
                    () -> registry.getBundle("amplitude-payment-client")
            );
        });
    }

    @Test
    void enabledWithoutCertificatesFailsClosed() {
        runner.withPropertyValues(
                "SIXPAY_BANKING_MTLS_ENABLED=true"
        ).run(context -> {

            assertNull(context.getStartupFailure());

            SslBundleRegistrar registrar =
                    context.getBean(SslBundleRegistrar.class);

            assertThrows(
                    IllegalStateException.class,
                    () -> registrar.registerBundles(
                            new DefaultSslBundleRegistry()
                    )
            );
        });
    }
}