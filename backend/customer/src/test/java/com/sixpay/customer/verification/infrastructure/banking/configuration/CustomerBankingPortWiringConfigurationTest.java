package com.sixpay.customer.verification.infrastructure.banking.configuration;

import com.sixpay.customer.configuration.CustomerCapabilityUnavailableException;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.management.infrastructure.banking.amplitude.AmplitudeCustomerLookupAdapter;
import com.sixpay.customer.verification.application.port.output.BankingCustomerVerificationPort;
import com.sixpay.customer.verification.configuration.CustomerVerificationApplicationConfiguration;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetryingBankingCustomerVerificationAdapter;
import com.sixpay.integration.http.StandardRestClientFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerBankingPortWiringConfigurationTest {

    private static final String PREFIX = "sixpay.customer.verification.banking.";

    private ApplicationContextRunner runner(boolean reverseOrder) {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withBean(StandardRestClientFactory.class,
                        () -> new StandardRestClientFactory(RestClient.builder()))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(ObjectMapper.class, ObjectMapper::new);
        return reverseOrder
                ? runner.withUserConfiguration(
                        AmplitudeCustomerVerificationConfiguration.class,
                        CustomerVerificationApplicationConfiguration.class)
                : runner.withUserConfiguration(
                        CustomerVerificationApplicationConfiguration.class,
                        AmplitudeCustomerVerificationConfiguration.class);
    }

    private static String[] validEnabledConfiguration() {
        return new String[] {
                PREFIX + "enabled=true",
                PREFIX + "base-url=http://127.0.0.1:9092",
                PREFIX + "endpoint-path=/api/v1/customer-verifications",
                PREFIX + "connect-timeout=2s",
                PREFIX + "read-timeout=5s",
                PREFIX + "max-attempts=3",
                PREFIX + "retry-backoff=250ms",
                PREFIX + "evidence-ttl=5m",
                PREFIX + "contract.version=1.0.0",
                PREFIX + "contract.success-codes[0]=00",
                PREFIX + "contract.business-failure-codes[0]=01"
        };
    }

    @Test
    void missingPropertyUsesOnlyFailClosedPorts() {
        runner(false).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(BankingCustomerLookupPort.class)).hasSize(1);
            assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class)).hasSize(1);
            assertThat(context).hasBean("unavailableBankingCustomerLookupPort");
            assertThat(context).hasBean("unavailableBankingCustomerVerificationPort");
            assertThat(context).doesNotHaveBean("bankingCustomerLookupPort");
            assertThat(context).doesNotHaveBean("bankingCustomerVerificationPort");
            assertLookupFailsClosed(context.getBean(BankingCustomerLookupPort.class));
        });
    }

    @Test
    void explicitlyDisabledPropertyUsesOnlyFailClosedPorts() {
        runner(false).withPropertyValues(PREFIX + "enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(BankingCustomerLookupPort.class)).hasSize(1);
                    assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class)).hasSize(1);
                    assertThat(context).hasBean("unavailableBankingCustomerLookupPort");
                    assertThat(context).hasBean("unavailableBankingCustomerVerificationPort");
                    assertLookupFailsClosed(context.getBean(BankingCustomerLookupPort.class));
                });
    }

    @Test
    void enabledPropertyUsesOnlyRealPortsRegardlessOfConfigurationOrder() {
        for (boolean reverseOrder : new boolean[] {false, true}) {
            runner(reverseOrder).withPropertyValues(validEnabledConfiguration())
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBeansOfType(BankingCustomerLookupPort.class))
                                .hasSize(1);
                        assertThat(context.getBeansOfType(BankingCustomerVerificationPort.class))
                                .hasSize(1);
                        assertThat(context.getBean(BankingCustomerLookupPort.class))
                                .isInstanceOf(AmplitudeCustomerLookupAdapter.class);
                        assertThat(context.getBean(BankingCustomerVerificationPort.class))
                                .isInstanceOf(RetryingBankingCustomerVerificationAdapter.class);
                        assertThat(context).doesNotHaveBean("unavailableBankingCustomerLookupPort");
                        assertThat(context).doesNotHaveBean("unavailableBankingCustomerVerificationPort");
                    });
        }
    }

    @Test
    void enabledButIncompleteBankingConfigurationFailsStartup() {
        runner(false).withPropertyValues(PREFIX + "enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    private static void assertLookupFailsClosed(BankingCustomerLookupPort port) {
        assertThatThrownBy(() -> port.lookup(
                new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                        "SIXPAY_BANK", "NIU-001", null, "ACC-001", "corr-001")))
                .isInstanceOf(CustomerCapabilityUnavailableException.class)
                .hasMessageContaining("banking lookup");
    }
}
