package com.sixpay.payment.infrastructure.outbox.mapper;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEventType;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionPayload;
import com.sixpay.payment.application.event.projection
        .ProjectionPaymentStatus;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Objects;

/**
 * Captures a versioned Observed Customer projection snapshot from Payment at
 * the moment its canonical domain event is persisted to the outbox.
 */
public final class PaymentObservedCustomerProjectionEventMapper {

    public ObservedCustomerProjectionEvent from(
            Payment payment,
            PaymentDomainEvent event
    ) {
        Objects.requireNonNull(
                payment,
                "payment is required"
        );
        Objects.requireNonNull(
                event,
                "event is required"
        );

        PaymentState state = payment.toState();

        if (!state.paymentId().equals(event.paymentId())) {
            throw new IllegalArgumentException(
                    "Payment and domain event identifiers differ"
            );
        }

        if (state.status() != event.paymentStatus()) {
            throw new IllegalArgumentException(
                    "Payment state must match the source event status"
            );
        }

        PaymentInitiationContext initiation =
                state.initiationContext()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment initiation context "
                                                + "is required for the "
                                                + "projection event"
                                )
                        );

        ObservedCustomerProjectionPayload payload =
                new ObservedCustomerProjectionPayload(
                        state.publicPaymentReference().value(),
                        initiation.taxpayerIdentifier(),
                        initiation.debtorName(),
                        null,
                        null,
                        state.financialInstitutionCode().value(),
                        state.debtorAccountReference()
                                .bindingFingerprint(),
                        state.debtorAccountReference()
                                .maskedDisplay(),
                        state.requestedAmount().amount(),
                        state.requestedAmount()
                                .currency()
                                .getCurrencyCode(),
                        mapStatus(state.status()),
                        state.failure()
                                .map(failure ->
                                        failure.failureCode().value()
                                )
                                .orElse(null),
                        state.receivedAt(),
                        state.updatedAt()
                );

        return ObservedCustomerProjectionEvent.versionOne(
                event.eventId(),
                state.paymentId().value(),
                event.aggregateVersion(),
                mapEventType(
                        event,
                        state.status()
                ),
                payload,
                event.correlationId().value(),
                event.occurredAt()
        );
    }

    private static ObservedCustomerProjectionEventType
            mapEventType(
                    PaymentDomainEvent event,
                    PaymentStatus status
            ) {
        if (event.eventSequence() == 1
                && status == PaymentStatus.RECEIVED) {
            return ObservedCustomerProjectionEventType
                    .PAYMENT_CREATED;
        }

        return switch (status) {
            case REJECTED ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_REJECTED;
            case FAILED ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_FAILED;
            case DEBIT_CONFIRMED ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_DEBIT_CONFIRMED;
            case REVERSED ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_REVERSED;
            case TREASURY_INTEGRATED ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_FINALIZED;
            default ->
                    ObservedCustomerProjectionEventType
                            .PAYMENT_STATUS_CHANGED;
        };
    }

    private static ProjectionPaymentStatus mapStatus(
            PaymentStatus status
    ) {
        return switch (status) {
            case RECEIVED,
                 PENDING_CONFIRMATION ->
                    ProjectionPaymentStatus.RECEIVED;
            case AUTHORIZATION_CHECKING ->
                    ProjectionPaymentStatus
                            .AUTHORIZATION_CHECKING;
            case BANKING_VERIFICATION_PENDING ->
                    ProjectionPaymentStatus.BANKING_CHECKING;
            case FUNDS_CONTROL_PENDING,
                 TREASURY_ACCOUNT_RESOLUTION_PENDING,
                 APPROVED_FOR_POSTING ->
                    ProjectionPaymentStatus.APPROVED;
            case POSTING_PENDING ->
                    ProjectionPaymentStatus.POSTING;
            case POSTING_OUTCOME_UNKNOWN,
                 REVERSAL_OUTCOME_UNKNOWN ->
                    ProjectionPaymentStatus
                            .ACCOUNTING_OUTCOME_UNKNOWN;
            case DEBIT_CONFIRMED ->
                    ProjectionPaymentStatus.DEBITED;
            case POSTED_PENDING_TFJ ->
                    ProjectionPaymentStatus.CUT_CREDITED;
            case REVERSAL_REQUIRED ->
                    ProjectionPaymentStatus
                            .REVERSAL_REQUIRED;
            case REVERSAL_PENDING ->
                    ProjectionPaymentStatus
                            .REVERSAL_PENDING;
            case REVERSED ->
                    ProjectionPaymentStatus.REVERSED;
            case REJECTED ->
                    ProjectionPaymentStatus.REJECTED;
            case FAILED ->
                    ProjectionPaymentStatus.FAILED;
            case TREASURY_INTEGRATED ->
                    ProjectionPaymentStatus
                            .TREASURY_INTEGRATED;
        };
    }
}
