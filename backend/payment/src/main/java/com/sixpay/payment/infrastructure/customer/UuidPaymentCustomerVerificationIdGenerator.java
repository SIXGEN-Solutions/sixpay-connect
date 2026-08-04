package com.sixpay.payment.infrastructure.customer;

import com.sixpay.payment.application.port.output.PaymentCustomerVerificationIdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class UuidPaymentCustomerVerificationIdGenerator
        implements PaymentCustomerVerificationIdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
