package com.sixpay.partner.domain.exception;

import com.sixpay.sharedkernel.domain.exception.DomainException;

public class PartnerDomainException extends DomainException {

    private static final String CODE = "PARTNER_RULE_VIOLATION";

    public PartnerDomainException(String message) {
        super(CODE, message);
    }
}
