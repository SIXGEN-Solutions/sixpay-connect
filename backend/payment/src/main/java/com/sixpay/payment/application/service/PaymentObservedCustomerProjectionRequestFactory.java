package com.sixpay.payment.application.service;

import com.sixpay.payment.application.event.projection.ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection.ProjectionPaymentStatus;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;

import java.util.Objects;

/**
 * Translates a durable Payment projection event into the Payment-owned output
 * port request. All projected state comes from the event payload.
 */
public final class PaymentObservedCustomerProjectionRequestFactory {

    public ObservedCustomerProjectionRequest from(
            ObservedCustomerProjectionEvent event
    ) {
        Objects.requireNonNull(event, "event is required");
        var payload = event.payload();

        return new ObservedCustomerProjectionRequest(
                event.eventId(),
                event.paymentId(),
                payload.paymentReference(),
                payload.normalizedNiu(),
                payload.legalName(),
                payload.phoneMasked(),
                payload.emailMasked(),
                payload.financialInstitutionCode(),
                payload.accountBindingFingerprint(),
                payload.maskedAccountReference(),
                payload.amount(),
                payload.currency(),
                mapStatus(payload.paymentStatus()),
                payload.failureReasonCode(),
                payload.paymentCreatedAt(),
                payload.paymentUpdatedAt(),
                event.occurredAt(),
                event.correlationId()
        );
    }

    private static ObservedCustomerProjectionRequest.ProjectionPaymentStatus mapStatus(
            ProjectionPaymentStatus status
    ) {
        return switch (status) {
            case RECEIVED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.RECEIVED;
            case AUTHORIZATION_CHECKING -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.AUTHORIZATION_CHECKING;
            case BANKING_CHECKING -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.BANKING_CHECKING;
            case REJECTED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.REJECTED;
            case APPROVED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.APPROVED;
            case POSTING -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.POSTING;
            case ACCOUNTING_OUTCOME_UNKNOWN -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.ACCOUNTING_OUTCOME_UNKNOWN;
            case DEBITED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.DEBITED;
            case CUT_CREDITED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.CUT_CREDITED;
            case REVERSAL_REQUIRED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.REVERSAL_REQUIRED;
            case REVERSAL_PENDING -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.REVERSAL_PENDING;
            case REVERSED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.REVERSED;
            case FAILED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.FAILED;
            case NOTIFIED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.NOTIFIED;
            case PENDING_END_OF_DAY_CONFIRMATION -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.PENDING_END_OF_DAY_CONFIRMATION;
            case TREASURY_INTEGRATED -> ObservedCustomerProjectionRequest.ProjectionPaymentStatus.TREASURY_INTEGRATED;
        };
    }
}
