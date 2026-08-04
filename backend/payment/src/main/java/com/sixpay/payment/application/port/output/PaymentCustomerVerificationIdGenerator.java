package com.sixpay.payment.application.port.output;

import java.util.UUID;

/**
 * Generates stable identifiers for Customer Verification requests initiated by
 * Payment.
 */
@FunctionalInterface
public interface PaymentCustomerVerificationIdGenerator {

    UUID nextId();
}
