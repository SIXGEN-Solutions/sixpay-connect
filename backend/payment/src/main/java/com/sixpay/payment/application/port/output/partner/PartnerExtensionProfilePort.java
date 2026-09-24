package com.sixpay.payment.application.port.output.partner;

import com.sixpay.payment.domain.model.CanonicalPartnerIdentity;

/**
 * Payment-owned output port. Payment never imports the Partner module.
 */
public interface PartnerExtensionProfilePort {
    PartnerExtensionProfile resolve(CanonicalPartnerIdentity partnerIdentity);
}
