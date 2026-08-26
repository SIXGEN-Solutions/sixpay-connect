package com.sixpay.administration.domain.model;

import java.util.Objects;

public record AdministrationSettings(
        String accountingCutoffZone,
        String accountingCutoffTime
) {
    public AdministrationSettings {
        Objects.requireNonNull(
                accountingCutoffZone,
                "accountingCutoffZone"
        );
        Objects.requireNonNull(
                accountingCutoffTime,
                "accountingCutoffTime"
        );
    }
}
