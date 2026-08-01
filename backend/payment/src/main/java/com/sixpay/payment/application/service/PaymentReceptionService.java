package com.sixpay.payment.application.service;

import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Receives one new Payment intent.
 */
@Service
public class PaymentReceptionService {

    private final PaymentRepository paymentRepository;
    private final PaymentMutationCoordinator coordinator;

    public PaymentReceptionService(
            PaymentRepository paymentRepository,
            PaymentMutationCoordinator coordinator
    ) {
        this.paymentRepository = Objects.requireNonNull(
                paymentRepository,
                "Payment repository"
        );
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    public PaymentWorkflowResult receive(
            PaymentId paymentId,
            PublicPaymentReference publicPaymentReference,
            NewPaymentIntent intent,
            Instant receivedAt
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        Objects.requireNonNull(intent, "New Payment intent");
        Objects.requireNonNull(receivedAt, "Received instant");

        if (paymentRepository
                .existsBySourceAndExternalPaymentReference(
                        intent.source(),
                        intent.externalPaymentReference()
                )) {
            throw new IllegalStateException(
                    "Payment already exists for source "
                            + intent.source()
                            + " and external reference "
                            + intent.externalPaymentReference()
            );
        }

        Payment payment = Payment.receive(
                paymentId,
                publicPaymentReference,
                intent,
                receivedAt
        );

        return coordinator.persistNew(payment);
    }
}
