package com.sixpay.administration.application.port.input;

import com.sixpay.administration.domain.model.AdministrationOverview;
import com.sixpay.administration.domain.model.AdministrationSettings;
import com.sixpay.administration.domain.model.IntegrationStatus;

import java.util.List;

public interface AdministrationQueryUseCase {

    AdministrationOverview overview();

    AdministrationSettings settings();

    List<IntegrationStatus> integrations();
}
