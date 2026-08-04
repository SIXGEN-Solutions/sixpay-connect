package com.sixpay.payment.application.port.output;

import com.sixpay.payment.domain.model.PaymentId;

import java.util.UUID;

/**
 * Derives the stable Customer Verification identifier owned by one Payment.
 */
@FunctionalInterface
public interface PaymentCustomerVerificationIdGenerator {

    UUID forPayment(PaymentId paymentId);
}
