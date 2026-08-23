package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.AdministrationSettings;

public interface AdministrationSettingsPort {

    AdministrationSettings load();
}
