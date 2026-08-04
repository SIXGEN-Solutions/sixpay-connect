package com.sixpay.payment.infrastructure.customer;

import com.sixpay.payment.application.port.output.PaymentCustomerVerificationIdGenerator;
import com.sixpay.payment.domain.model.PaymentId;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * Stable name-based identifier generator.
 *
 * <p>The same Payment always produces the same Customer Verification ID,
 * including after process restart or durable-message replay.</p>
 */
@Component
public final class DeterministicPaymentCustomerVerificationIdGenerator
        implements PaymentCustomerVerificationIdGenerator {

    private static final String NAMESPACE =
            "sixpay:payment:customer-verification:v1:";

    @Override
    public UUID forPayment(PaymentId paymentId) {
        Objects.requireNonNull(paymentId, "paymentId is required");

        return UUID.nameUUIDFromBytes(
                (NAMESPACE + paymentId.value())
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
