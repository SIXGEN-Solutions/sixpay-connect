package com.sixpay.administration.infrastructure.configuration;

import com.sixpay.administration.application.port.output.AdministrationSettingsPort;
import com.sixpay.administration.domain.model.AdministrationSettings;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

@Component
public class SpringConfigurationAdministrationSettingsAdapter
        implements AdministrationSettingsPort {

    static final String CUTOFF_ZONE =
            "sixpay.accounting.batch.cutoff-zone";

    static final String CUTOFF_TIME =
            "sixpay.accounting.batch.cutoff-time";

    private static final String DEFAULT_ZONE =
            "Africa/Douala";

    private static final String DEFAULT_TIME =
            "23:00";

    private final Environment environment;

    public SpringConfigurationAdministrationSettingsAdapter(
            Environment environment
    ) {
        this.environment =
                Objects.requireNonNull(environment);
    }

    @Override
    public AdministrationSettings load() {
        ZoneId cutoffZone = ZoneId.of(
                environment.getProperty(
                        CUTOFF_ZONE,
                        DEFAULT_ZONE
                )
        );

        LocalTime cutoffTime = LocalTime.parse(
                environment.getProperty(
                        CUTOFF_TIME,
                        DEFAULT_TIME
                )
        );

        return new AdministrationSettings(
                cutoffZone.getId(),
                cutoffTime.toString()
        );
    }
}
