package com.sixpay.payment.application.port.output.security;

import com.sixpay.payment.application.security.PaymentObjectAccessDescriptor;
import com.sixpay.payment.domain.model.PaymentId;

import java.util.Optional;

/**
 * Reads the minimal ownership metadata required for Payment object access.
 */
public interface PaymentObjectAccessPort {

    Optional<PaymentObjectAccessDescriptor> findAccessDescriptor(
            PaymentId paymentId
    );
}
