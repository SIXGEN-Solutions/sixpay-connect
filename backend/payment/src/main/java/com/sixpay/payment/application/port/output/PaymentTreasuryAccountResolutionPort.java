package com.sixpay.payment.application.port.output;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.TreasuryAccountReference;

import java.time.Instant;

@FunctionalInterface
public interface PaymentTreasuryAccountResolutionPort {

    TreasuryAccountReference resolve(
            Payment payment,
            Instant requestedAt
    );
}
