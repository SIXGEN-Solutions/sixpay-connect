package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionRequest;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionResult;

import java.util.Objects;

/**
 * Composition-layer adapter between the Payment-owned projection contract and
 * Customer Observation.
 */
public final class ObservedCustomerProjectionModuleAdapter
        implements ObservedCustomerProjectionPort {

    private final ObserveCustomerUseCase observeCustomerUseCase;

    public ObservedCustomerProjectionModuleAdapter(
            ObserveCustomerUseCase observeCustomerUseCase
    ) {
        this.observeCustomerUseCase = Objects.requireNonNull(
                observeCustomerUseCase,
                "observeCustomerUseCase is required"
        );
    }

    @Override
    public ObservedCustomerProjectionResult project(
            ObservedCustomerProjectionRequest request
    ) {
        Objects.requireNonNull(request, "request is required");

        ObserveCustomerResult result =
                observeCustomerUseCase.observe(
                        new ObserveCustomerCommand(
                                request.sourceEventId(),
                                request.paymentId(),
                                request.paymentReference(),
                                request.normalizedNiu(),
                                request.legalName(),
                                request.phoneMasked(),
                                request.emailMasked(),
                                request.financialInstitutionCode(),
                                request.accountBindingFingerprint(),
                                request.maskedAccountReference(),
                                request.amount(),
                                request.currency(),
                                mapStatus(request.paymentStatus()),
                                request.failureReasonCode(),
                                request.paymentCreatedAt(),
                                request.paymentUpdatedAt(),
                                request.observedAt(),
                                request.correlationId()
                        )
                );

        return new ObservedCustomerProjectionResult(
                result.sourceEventId(),
                switch (result.disposition()) {
                    case APPLIED ->
                            ObservedCustomerProjectionResult
                                    .Disposition.APPLIED;
                    case REPLAYED ->
                            ObservedCustomerProjectionResult
                                    .Disposition.REPLAYED;
                    case IGNORED_STALE ->
                            ObservedCustomerProjectionResult
                                    .Disposition.IGNORED_STALE;
                },
                result.projectionVersion()
        );
    }

    private static ObservedPaymentStatus mapStatus(
            ObservedCustomerProjectionRequest
                    .ProjectionPaymentStatus status
    ) {
        return switch (status) {
            case RECEIVED -> ObservedPaymentStatus.RECEIVED;
            case AUTHORIZATION_CHECKING ->
                    ObservedPaymentStatus.AUTHORIZATION_CHECKING;
            case BANKING_CHECKING ->
                    ObservedPaymentStatus.BANKING_CHECKING;
            case REJECTED -> ObservedPaymentStatus.REJECTED;
            case APPROVED -> ObservedPaymentStatus.APPROVED;
            case POSTING -> ObservedPaymentStatus.POSTING;
            case ACCOUNTING_OUTCOME_UNKNOWN ->
                    ObservedPaymentStatus
                            .ACCOUNTING_OUTCOME_UNKNOWN;
            case DEBITED -> ObservedPaymentStatus.DEBITED;
            case CUT_CREDITED ->
                    ObservedPaymentStatus.CUT_CREDITED;
            case REVERSAL_REQUIRED ->
                    ObservedPaymentStatus.REVERSAL_REQUIRED;
            case REVERSAL_PENDING ->
                    ObservedPaymentStatus.REVERSAL_PENDING;
            case REVERSED -> ObservedPaymentStatus.REVERSED;
            case FAILED -> ObservedPaymentStatus.FAILED;
            case NOTIFIED -> ObservedPaymentStatus.NOTIFIED;
            case PENDING_END_OF_DAY_CONFIRMATION ->
                    ObservedPaymentStatus
                            .PENDING_END_OF_DAY_CONFIRMATION;
            case TREASURY_INTEGRATED ->
                    ObservedPaymentStatus.TREASURY_INTEGRATED;
        };
    }
}
