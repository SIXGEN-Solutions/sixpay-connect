package com.sixpay.partner.application.port.input;

import com.sixpay.partner.application.view.PartnerIdentityView;

import java.util.Optional;

public interface PartnerIdentityQueryUseCase {
    Optional<PartnerIdentityView> findByPartnerIdentifier(String partnerIdentifier);
}
