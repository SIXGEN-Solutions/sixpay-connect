package com.sixpay.payment.application.port;

import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.policy.PaymentAuthorizationRuntimeView;

/** Runtime authorization evidence obtained through Customer, Partner and Security public APIs. */
public interface PaymentAuthorizationRuntimePort {
    PaymentAuthorizationRuntimeView resolve(PaymentState state);
}
