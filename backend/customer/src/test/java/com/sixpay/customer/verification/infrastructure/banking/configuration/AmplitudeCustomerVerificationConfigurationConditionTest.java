package com.sixpay.customer.verification.infrastructure.banking.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AmplitudeCustomerVerificationConfigurationConditionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    AmplitudeCustomerVerificationConfiguration.class
                            )
                    );

    @Test
    void bankingBeansAreDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(
                    "coreBankingAccessTokenProvider"
            );
            assertThat(context).doesNotHaveBean(
                    "bankingCustomerVerificationPort"
            );
        });
    }
}
