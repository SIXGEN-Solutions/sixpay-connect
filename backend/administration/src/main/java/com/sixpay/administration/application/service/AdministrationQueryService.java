package com.sixpay.administration.application.service;

import com.sixpay.administration.application.port.input.AdministrationQueryUseCase;
import com.sixpay.administration.application.port.output.AdministrationSettingsPort;
import com.sixpay.administration.application.port.output.IntegrationHealthQueryPort;
import com.sixpay.administration.domain.model.AdministrationOverview;
import com.sixpay.administration.domain.model.AdministrationSettings;
import com.sixpay.administration.domain.model.IntegrationStatus;
import com.sixpay.common.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AdministrationQueryService
        implements AdministrationQueryUseCase {

    private final AdministrationSettingsPort settingsPort;
    private final IntegrationHealthQueryPort healthQueryPort;
    private final TimeProvider timeProvider;

    public AdministrationQueryService(
            AdministrationSettingsPort settingsPort,
            IntegrationHealthQueryPort healthQueryPort,
            TimeProvider timeProvider
    ) {
        this.settingsPort =
                Objects.requireNonNull(settingsPort);
        this.healthQueryPort =
                Objects.requireNonNull(healthQueryPort);
        this.timeProvider =
                Objects.requireNonNull(timeProvider);
    }

    @Override
    public AdministrationOverview overview() {
        return new AdministrationOverview(
                settingsPort.load(),
                healthQueryPort.findAll(),
                timeProvider.now()
        );
    }

    @Override
    public AdministrationSettings settings() {
        return settingsPort.load();
    }

    @Override
    public List<IntegrationStatus> integrations() {
        return List.copyOf(
                healthQueryPort.findAll()
        );
    }
}
