package com.sixpay.payment.application.service;

import com.sixpay.payment.application.view.PaymentConsultationView;
import com.sixpay.payment.application.port.input.PaymentConsultationUseCase;
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
public final class PaymentConsultationService
        implements PaymentConsultationUseCase {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentConfirmationPartnerAccessGuard accessGuard;

    public PaymentConsultationService(
            PaymentLookupPort paymentLookupPort,
            PaymentConfirmationPartnerAccessGuard accessGuard
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.accessGuard = Objects.requireNonNull(accessGuard, "Partner access guard");
    }

    @Override
    public Optional<PaymentConsultationView> findByPaymentReference(
            PublicPaymentReference paymentReference
    ) {
        Objects.requireNonNull(paymentReference, "Public Payment reference");
        // Deny non-owned and unknown Payment references indistinguishably.
        // This also rejects non-M2M callers; the HTTP scope is checked separately.
        if (!accessGuard.isAllowed(paymentReference.value())) {
            return Optional.empty();
        }

        return paymentLookupPort
                .findByPublicPaymentReference(paymentReference)
                .map(PaymentConsultationService::toResponse);
    }

    private static PaymentConsultationView toResponse(
            Payment payment
    ) {
        var state = payment.toState();

        return new PaymentConsultationView(
                state.paymentId().value(),
                state.publicPaymentReference().value(),
                state.externalPaymentReference().value(),
                state.status().name(),
                new PaymentConsultationView.Money(
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
