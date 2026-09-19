package com.sixpay.payment.application.service;

import com.sixpay.payment.application.view.PaymentRecoveryView;
import com.sixpay.payment.application.port.input.PaymentRecoveryUseCase;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Read-only recovery view backed by the authoritative Payment aggregate.
 *
 * <p>No banking verification, OTP, posting or financial recovery operation is
 * triggered by this service.</p>
 */
@Service
public final class PaymentRecoveryService
        implements PaymentRecoveryUseCase {

    private final PaymentLookupPort paymentLookupPort;

    public PaymentRecoveryService(
            PaymentLookupPort paymentLookupPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
    }

    @Override
    public Optional<PaymentRecoveryView> findByPaymentReference(
            PublicPaymentReference paymentReference
    ) {
        Objects.requireNonNull(paymentReference, "Public Payment reference");

        return paymentLookupPort
                .findByPublicPaymentReference(paymentReference)
                .map(PaymentRecoveryService::toResponse);
    }

    private static PaymentRecoveryView toResponse(
            Payment payment
    ) {
        var state = payment.toState();

        return new PaymentRecoveryView(
                state.paymentId().value(),
                state.publicPaymentReference().value(),
                state.externalPaymentReference().value(),
                state.status().name(),
                new PaymentRecoveryView.Money(
                        state.requestedAmount().amount(),
                        state.requestedAmount()
                                .currency()
                                .getCurrencyCode()
                ),
                state.receivedAt(),
                state.updatedAt(),
                state.finalizedAt().orElse(null)
        );
    }
}
