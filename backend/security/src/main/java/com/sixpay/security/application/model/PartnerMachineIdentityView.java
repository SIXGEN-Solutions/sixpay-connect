package com.sixpay.security.application.model;

import com.sixpay.common.validation.Preconditions;

/**
 * Public Security projection linking one trusted machine subject to a Partner
 * business identifier.
 */
public record PartnerMachineIdentityView(
        String machineSubject,
        String partnerIdentifier
) {

    public PartnerMachineIdentityView {
        machineSubject = Preconditions.requireNonBlank(
                machineSubject,
                "Machine subject must not be blank"
        );
        partnerIdentifier = Preconditions.requireNonBlank(
                partnerIdentifier,
                "Partner identifier must not be blank"
        );
    }
}
