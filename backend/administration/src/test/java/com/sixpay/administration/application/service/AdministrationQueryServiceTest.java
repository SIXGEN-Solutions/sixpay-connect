package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.output.AdministrationSettingsPort;
import com.sixpay.administration.application.port.output.IntegrationHealthQueryPort;
import com.sixpay.administration.domain.model.AdministrationSettings;
import com.sixpay.administration.domain.model.IntegrationHealth;
import com.sixpay.administration.domain.model.IntegrationStatus;
import com.sixpay.common.time.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationQueryServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-23T14:00:00Z");

    @Test
    void buildsOverviewFromRealPorts() {
        AdministrationSettings settings =
                new AdministrationSettings(
                        "Africa/Douala",
                        "23:00"
                );

        IntegrationStatus integration =
                new IntegrationStatus(
                        "db",
                        "PostgreSQL",
                        "DATABASE",
                        IntegrationHealth.AVAILABLE,
                        null,
                        null,
                        NOW
                );

        AdministrationSettingsPort settingsPort =
                () -> settings;

        IntegrationHealthQueryPort healthPort =
                () -> List.of(integration);

        TimeProvider timeProvider =
                () -> NOW;

        AdministrationQueryService service =
                new AdministrationQueryService(
                        settingsPort,
                        healthPort,
                        timeProvider
                );

        var overview = service.overview();

        assertThat(overview.settings())
                .isEqualTo(settings);

        assertThat(overview.integrations())
                .containsExactly(integration);

        assertThat(overview.observedAt())
                .isEqualTo(NOW);
    }

    @Test
    void delegatesSettingsAndIntegrationsQueries() {
        AdministrationSettings settings =
                new AdministrationSettings(
                        "Africa/Douala",
                        "23:00"
                );

        IntegrationStatus integration =
                new IntegrationStatus(
                        "coreBanking",
                        "Core Banking",
                        "CORE_BANKING",
                        IntegrationHealth.UNKNOWN,
                        null,
                        null,
                        NOW
                );

        AdministrationQueryService service =
                new AdministrationQueryService(
                        () -> settings,
                        () -> List.of(integration),
                        () -> NOW
                );

        assertThat(service.settings())
                .isEqualTo(settings);

        assertThat(service.integrations())
                .containsExactly(integration);
    }
}
