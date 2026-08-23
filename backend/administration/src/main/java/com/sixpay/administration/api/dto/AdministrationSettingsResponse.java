package com.sixpay.administration.api.dto;

import com.sixpay.administration.domain.model.AdministrationSettings;

public record AdministrationSettingsResponse(
        String accountingCutoffZone,
        String accountingCutoffTime
) {
    public static AdministrationSettingsResponse from(
            AdministrationSettings settings
    ) {
        return new AdministrationSettingsResponse(
                settings.accountingCutoffZone(),
                settings.accountingCutoffTime()
        );
    }
}
