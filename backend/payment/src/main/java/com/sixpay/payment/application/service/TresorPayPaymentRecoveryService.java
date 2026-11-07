package com.sixpay.payment.application.service;

import com.sixpay.payment.application.view.TresorPayPaymentRecoveryView;
import com.sixpay.payment.application.port.input.TresorPayPaymentRecoveryUseCase;
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
public final class TresorPayPaymentRecoveryService
        implements TresorPayPaymentRecoveryUseCase {

    private final PaymentLookupPort paymentLookupPort;

    public TresorPayPaymentRecoveryService(
            PaymentLookupPort paymentLookupPort
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
    }

    @Override
    public Optional<TresorPayPaymentRecoveryView> findByPaymentReference(
            PublicPaymentReference paymentReference
    ) {
        Objects.requireNonNull(paymentReference, "Public Payment reference");

        return paymentLookupPort
                .findByPublicPaymentReference(paymentReference)
                .map(TresorPayPaymentRecoveryService::toResponse);
    }

    private static TresorPayPaymentRecoveryView toResponse(
            Payment payment
    ) {
        var state = payment.toState();

        return new TresorPayPaymentRecoveryView(
                state.paymentId().value(),
                state.publicPaymentReference().value(),
                state.externalPaymentReference().value(),
                state.status().name(),
                new TresorPayPaymentRecoveryView.Money(
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
