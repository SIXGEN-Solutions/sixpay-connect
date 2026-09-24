package com.sixpay.partner.application.service;

import com.sixpay.partner.application.contract.PartnerExtensionProfileQuery;
import com.sixpay.partner.application.contract.PartnerExtensionProfileView;
import com.sixpay.partner.application.contract.PartnerIdentity;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * PA-2 fail-closed surface. No storage model is invented in PA-2.
 */
@Service
public final class PartnerExtensionProfileQueryService
        implements PartnerExtensionProfileQuery {

    @Override
    public PartnerExtensionProfileView resolve(PartnerIdentity partnerIdentity) {
        Objects.requireNonNull(partnerIdentity, "Partner identity");
        return PartnerExtensionProfileView.empty();
    }
}
