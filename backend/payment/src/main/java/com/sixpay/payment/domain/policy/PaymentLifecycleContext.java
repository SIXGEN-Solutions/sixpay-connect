package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Objects;

public record PaymentLifecycleContext(
        PaymentStatus status,
        boolean terminal
) {
    public PaymentLifecycleContext {
        status = Objects.requireNonNull(status, "Payment status");
        if (terminal != status.isTerminal()) {
            throw new IllegalArgumentException(
                    "Terminal flag must match PaymentStatus"
            );
        }
    }

    public static PaymentLifecycleContext of(PaymentStatus status) {
        return new PaymentLifecycleContext(status, status.isTerminal());
    }
}
