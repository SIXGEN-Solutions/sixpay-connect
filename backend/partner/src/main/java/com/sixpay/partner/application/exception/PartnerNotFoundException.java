package com.sixpay.partner.application.exception;

import com.sixpay.partner.domain.model.PartnerId;

public class PartnerNotFoundException extends RuntimeException {

    public PartnerNotFoundException(PartnerId partnerId) {
        super("partner not found: " + partnerId);
    }
}
