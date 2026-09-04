package com.sixpay.payment.application.port;

import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.policy.PartnerAuthorizationView;

public interface PartnerAuthorizationPort {

    PartnerAuthorizationView resolve(
            PaymentInitiationContext initiationContext
    );
}
