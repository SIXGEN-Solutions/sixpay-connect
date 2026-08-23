package com.sixpay.administration.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SpringConfigurationAdministrationSettingsAdapterTest {

    @Test
    void usesSameDefaultsAsAccountingBatchProperties() {
        var adapter =
                new SpringConfigurationAdministrationSettingsAdapter(
                        new MockEnvironment()
                );

        var settings = adapter.load();

        assertThat(settings.accountingCutoffZone())
                .isEqualTo("Africa/Douala");

        assertThat(settings.accountingCutoffTime())
                .isEqualTo("23:00");
    }

    @Test
    void readsEffectiveSpringConfiguration() {
        MockEnvironment environment =
                new MockEnvironment()
                        .withProperty(
                                SpringConfigurationAdministrationSettingsAdapter
                                        .CUTOFF_ZONE,
                                "UTC"
                        )
                        .withProperty(
                                SpringConfigurationAdministrationSettingsAdapter
                                        .CUTOFF_TIME,
                                "22:30"
                        );

        var settings =
                new SpringConfigurationAdministrationSettingsAdapter(
                        environment
                ).load();

        assertThat(settings.accountingCutoffZone())
                .isEqualTo("UTC");

        assertThat(settings.accountingCutoffTime())
                .isEqualTo("22:30");
    }
}
