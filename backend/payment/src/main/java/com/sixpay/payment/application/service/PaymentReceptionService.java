package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.NewPaymentIntent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/**
 * Creates the Payment aggregate for a prepared initiation and advances it to
 * the customer-confirmation waiting state.
 *
 * <p>The external payment reference is checked before creation for a clearer
 * application error; the database unique constraint remains the final guard
 * against concurrent duplicates.</p>
 */
@Service
public class PaymentReceptionService {

    private final PaymentLookupPort paymentLookupPort;
    private final PaymentMutationCoordinator coordinator;

    public PaymentReceptionService(
            PaymentLookupPort paymentLookupPort,
            PaymentMutationCoordinator coordinator
    ) {
        this.paymentLookupPort = Objects.requireNonNull(
                paymentLookupPort,
                "Payment lookup port"
        );
        this.coordinator = Objects.requireNonNull(
                coordinator,
                "Payment mutation coordinator"
        );
    }

    /**
     * Creates a Payment, records reception and confirmation-request domain
     * events, then persists the aggregate, audit trail and outbox atomically.
     */
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

        if (paymentLookupPort
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
        payment.requestCustomerConfirmation(receivedAt);

        return coordinator.persistNew(payment);
    }
}
