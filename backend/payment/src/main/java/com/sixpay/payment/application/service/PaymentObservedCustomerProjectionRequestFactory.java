package com.sixpay.payment.application.service;

import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentInitiationContext;
import com.sixpay.payment.domain.model.PaymentState;
import com.sixpay.payment.domain.model.PaymentStatus;

import java.util.Objects;

/**
 * Builds the Payment-owned projection request from the current aggregate state
 * and canonical domain-event metadata.
 */
public final class PaymentObservedCustomerProjectionRequestFactory {

    public ObservedCustomerProjectionRequest from(
            Payment payment,
            PaymentDomainEvent event
    ) {
        Objects.requireNonNull(payment, "payment is required");
        Objects.requireNonNull(event, "event is required");

        PaymentState state = payment.toState();

        PaymentInitiationContext initiation =
                state.initiationContext()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment initiation context is "
                                                + "required for customer "
                                                + "observation"
                                )
                        );

        return new ObservedCustomerProjectionRequest(
                event.eventId(),
                state.paymentId().value(),
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
                state.updatedAt(),
                event.occurredAt(),
                event.correlationId().value()
        );
    }

    private static ObservedCustomerProjectionRequest
            .ProjectionPaymentStatus mapStatus(
                    PaymentStatus status
            ) {
        return switch (status) {
            case RECEIVED, PENDING_CONFIRMATION ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.RECEIVED;
            case AUTHORIZATION_CHECKING ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus
                            .AUTHORIZATION_CHECKING;
            case BANKING_VERIFICATION_PENDING ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.BANKING_CHECKING;
            case FUNDS_CONTROL_PENDING,
                 TREASURY_ACCOUNT_RESOLUTION_PENDING,
                 APPROVED_FOR_POSTING ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.APPROVED;
            case POSTING_PENDING ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.POSTING;
            case POSTING_OUTCOME_UNKNOWN,
                 REVERSAL_OUTCOME_UNKNOWN ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus
                            .ACCOUNTING_OUTCOME_UNKNOWN;
            case DEBIT_CONFIRMED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.DEBITED;
            case POSTED_PENDING_TFJ ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.CUT_CREDITED;
            case REVERSAL_REQUIRED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.REVERSAL_REQUIRED;
            case REVERSAL_PENDING ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.REVERSAL_PENDING;
            case REVERSED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.REVERSED;
            case REJECTED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.REJECTED;
            case FAILED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.FAILED;
            case TREASURY_INTEGRATED ->
                    ObservedCustomerProjectionRequest
                            .ProjectionPaymentStatus.TREASURY_INTEGRATED;
        };
    }
}
