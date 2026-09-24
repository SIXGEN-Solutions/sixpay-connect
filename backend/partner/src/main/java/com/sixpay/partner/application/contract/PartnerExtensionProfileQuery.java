package com.sixpay.partner.application.contract;

public interface PartnerExtensionProfileQuery {
    PartnerExtensionProfileView resolve(PartnerIdentity partnerIdentity);
}
