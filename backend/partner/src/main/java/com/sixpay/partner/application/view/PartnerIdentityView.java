package com.sixpay.partner.application.view;

import com.sixpay.partner.domain.model.Partner;
import com.sixpay.partner.domain.model.PartnerStatus;

import java.util.UUID;

public record PartnerIdentityView(
        UUID partnerId,
        String partnerIdentifier,
        PartnerStatus status
) {
    public static PartnerIdentityView from(Partner partner) {
        return new PartnerIdentityView(
                partner.id().value(),
                partner.partnerIdentifier().value(),
                partner.status()
        );
    }

    public boolean acceptsNewTransactions() {
        return status == PartnerStatus.ACTIVE;
    }
}
